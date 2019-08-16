package org.elasticsoftware.elasticactors.plugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType.ClassResolveResult;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static com.intellij.psi.util.InheritanceUtil.isInheritor;
import static com.intellij.psi.util.PsiTypesUtil.getParameterType;
import static com.intellij.psi.util.PsiTypesUtil.getPsiClass;
import static com.intellij.psi.util.PsiUtil.resolveGenericsClassInType;
import static org.elasticsoftware.elasticactors.Utils.isActorRefMethod;
import static org.elasticsoftware.elasticactors.Utils.isHandler;

public class MessageHandlerAnnotator implements Annotator {

    @Override
    public void annotate(
            @NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;
            if (isHandler(method)) {
                List<String> invalidReasons = validateArguments(method.getParameterList());
                if (!method.hasModifier(JvmModifier.PUBLIC)) {
                    holder.createErrorAnnotation(
                            element,
                            "Message Handler methods must be public");
                }
                if (invalidReasons != null) {
                    invalidReasons.forEach(s -> holder.createErrorAnnotation(element, s));
                }
                if (!PsiType.VOID.equals(method.getReturnType())) {
                    holder.createWarningAnnotation(
                            element,
                            "Message Handler methods should return void");
                }
            }
        } else if (element instanceof PsiParameter) {
            PsiParameter parameter = (PsiParameter) element;
            if (parameter.getDeclarationScope() instanceof PsiMethod) {
                PsiMethod method = (PsiMethod) parameter.getDeclarationScope();
                if (isHandler(method)) {
                    PsiType paramType = parameter.getType();
                    if (paramType instanceof PsiPrimitiveType) {
                        holder.createErrorAnnotation(parameter, "Primitive types not allowed here");
                        return;
                    }
                    if (!isValidHandlerParameterType(paramType)) {
                        holder.createErrorAnnotation(
                                parameter,
                                "Unexpected parameter type for handler method: "
                                        + parameter.getType().getCanonicalText());
                    }
                    if (parameter.isVarArgs()) {
                        holder.createErrorAnnotation(
                                parameter,
                                "Cannot use varargs in handler method");
                    }
                }
            }
        } else if (element instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;
            PsiMethod method = methodCall.resolveMethod();
            if (method != null) {
                boolean isTell = method.getName().equals("tell");
                boolean isAsk = !isTell && method.getName().equals("ask");
                if ((isTell || isAsk)
                        && isActorRef(method.getContainingClass())
                        && isActorRefMethod(method)) {
                    if (isTell) {
                        validateMessageArgument(holder, methodCall);
                    } else {
                        validateMessageArgument(holder, methodCall);
                        validateResponseTypeArgument(holder, methodCall);
                    }
                }
            }
        }
    }

    private static void validateMessageArgument(
            @NotNull AnnotationHolder holder,
            @NotNull PsiMethodCallExpression methodCall) {
        PsiExpression argument = methodCall.getArgumentList().getExpressions()[0];
        validateClass(holder, argument, argument.getType(), () -> getPsiClass(argument.getType()));
    }

    private static void validateResponseTypeArgument(
            @NotNull AnnotationHolder holder,
            @NotNull PsiMethodCallExpression methodCall) {
        PsiExpression argument = methodCall.getArgumentList().getExpressions()[1];
        ClassResolveResult resolveResult = resolveGenericsClassInType(argument.getType());
        if (resolveResult.getElement() != null) {
            validateClass(holder, argument, argument.getType(),
                    () -> getPsiClass(resolveResult.getSubstitutor()
                            .substitute(resolveResult.getElement().getTypeParameters()[0])));
        }
    }

    private static void validateClass(
            @NotNull AnnotationHolder holder,
            @NotNull PsiExpression argument,
            @Nullable PsiType type,
            @NotNull Supplier<PsiClass> classSupplier) {
        if (type instanceof PsiPrimitiveType) {
            holder.createErrorAnnotation(argument, "Primitive types not allowed here");
            return;
        }
        PsiClass argClass = classSupplier.get();
        if (isNotMessage(argClass)) {
            if (isConcrete(argClass)) {
                holder.createWarningAnnotation(
                        argument,
                        "Argument should be of a type annotated with @Message");
            }
            if (!isConcrete(argClass) || !argClass.hasModifier(JvmModifier.FINAL)) {
                ClassInheritorsSearch.search(argClass).findAll().stream()
                        .filter(MessageHandlerAnnotator::isConcrete)
                        .filter(MessageHandlerAnnotator::isNotMessage)
                        .forEach(psiClass -> holder.createWarningAnnotation(
                                argument,
                                "Found possible inheritor not annotated with @Message: "
                                        + psiClass.getQualifiedName()));
            }
        }
    }

    private static boolean isConcrete(PsiClass argClass) {
        return !argClass.isInterface() && !argClass.hasModifier(JvmModifier.ABSTRACT);
    }

    private static boolean isValidHandlerParameterType(@Nullable PsiType paramType) {
        PsiClass paramClass = getPsiClass(paramType);
        return isMessage(paramClass)
                || isActorRef(paramClass)
                || isActorState(paramClass)
                || isActorSystem(paramClass);
    }

    private static List<String> validateArguments(PsiParameterList psiParameterList) {
        int foundMessage = 0;
        int foundSender = 0;
        int foundState = 0;
        int foundActorSystem = 0;
        PsiParameter[] parameters = psiParameterList.getParameters();
        for (int i = 0; i < psiParameterList.getParametersCount(); i++) {
            PsiClass paramClass =
                    getPsiClass(getParameterType(parameters, i, parameters[i].isVarArgs()));
            if (isMessage(paramClass)) {
                foundMessage++;
            } else if (isActorRef(paramClass)) {
                foundSender++;
            } else if (isActorState(paramClass)) {
                foundState++;
            } else if (isActorSystem(paramClass)) {
                foundActorSystem++;
            }
        }
        if (psiParameterList.getParametersCount() == 0
                || foundMessage != 1
                || foundSender > 1
                || foundState > 1
                || foundActorSystem > 1) {
            List<String> reasons = new ArrayList<>();
            if (psiParameterList.getParametersCount() == 0) {
                reasons.add("Handler Method should have at least one parameter (message)");
            }
            if (foundMessage == 0) {
                reasons.add("Handler Method should have at least one parameter annotated with "
                        + "@Message");
            }
            if (foundMessage > 1) {
                reasons.add("Handler Method should have only one parameter annotated with "
                        + "@Message");
            }
            if (foundSender > 1) {
                reasons.add(
                        "Handler Method should not have more than one parameter of type "
                                + "org.elasticsoftware.elasticactors.ActorRef");
            }
            if (foundState > 1) {
                reasons.add(
                        "Handler Method should not have more than one parameter of type "
                                + "org.elasticsoftware.elasticactors.ActorState");
            }
            if (foundActorSystem > 1) {
                reasons.add("Handler Method should not have more than one parameter of type "
                        + "org.elasticsoftware.elasticactors.ActorSystem");
            }
            return reasons;
        }

        return null;
    }

    private static boolean isActorState(@Nullable PsiClass paramClass) {
        return isInheritor(
                paramClass,
                "org.elasticsoftware.elasticactors.ActorState");
    }

    private static boolean isActorSystem(@Nullable PsiClass paramClass) {
        return paramClass != null && Objects.equals(
                paramClass.getQualifiedName(),
                "org.elasticsoftware.elasticactors.ActorSystem");
    }

    private static boolean isActorRef(@Nullable PsiClass paramClass) {
        return paramClass != null && Objects.equals(
                paramClass.getQualifiedName(),
                "org.elasticsoftware.elasticactors.ActorRef");
    }

    private static boolean isNotMessage(@Nullable PsiClass argClass) {
        return argClass != null && !isMessage(argClass);
    }

    private static boolean isMessage(@Nullable PsiClass argClass) {
        return argClass != null && argClass.hasAnnotation(
                "org.elasticsoftware.elasticactors.serialization.Message");
    }

}
