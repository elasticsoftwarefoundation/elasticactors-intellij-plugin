package org.elasticsoftware.elasticactors.plugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.intellij.psi.util.InheritanceUtil.isInheritor;
import static com.intellij.psi.util.PsiTypesUtil.getParameterType;
import static com.intellij.psi.util.PsiTypesUtil.getPsiClass;
import static org.elasticsoftware.elasticactors.Utils.isHandler;
import static org.elasticsoftware.elasticactors.Utils.isMessage;

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
        }
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

}
