package org.elasticsoftware.elasticactors.plugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.intellij.lang.annotation.HighlightSeverity.ERROR;
import static com.intellij.lang.annotation.HighlightSeverity.WARNING;
import static com.intellij.psi.PsiModifier.PUBLIC;
import static com.intellij.psi.util.InheritanceUtil.isInheritor;
import static com.intellij.psi.util.PsiTypesUtil.getParameterType;
import static com.intellij.psi.util.PsiTypesUtil.getPsiClass;
import static org.elasticsoftware.elasticactors.Utils.ACTOR_REF_CLASS;
import static org.elasticsoftware.elasticactors.Utils.ACTOR_STATE_CLASS;
import static org.elasticsoftware.elasticactors.Utils.ACTOR_SYSTEM_CLASS;
import static org.elasticsoftware.elasticactors.Utils.ELASTIC_ACTOR_CLASS;
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
                if (!method.hasModifierProperty(PUBLIC)) {
                    holder.newAnnotation(ERROR, "Message Handler methods must be public")
                            .range(element)
                            .create();
                }
                if (invalidReasons != null) {
                    invalidReasons.forEach(s -> holder.newAnnotation(ERROR, s)
                            .range(element)
                            .create());
                }
                if (!PsiTypes.voidType().equals(method.getReturnType())) {
                    holder.newAnnotation(WARNING, "Message Handler methods should return void")
                            .range(element)
                            .create();
                }
            }
        } else if (element instanceof PsiParameter parameter) {
            if (parameter.getDeclarationScope() instanceof PsiMethod method) {
                if (isHandler(method)) {
                    PsiType paramType = parameter.getType();
                    if (!isValidHandlerParameterType(paramType)) {
                        String message = "Unexpected parameter type for handler method: "
                                + parameter.getType().getCanonicalText();
                        holder.newAnnotation(ERROR, message)
                                .range(parameter)
                                .create();
                    }
                    if (parameter.isVarArgs()) {
                        holder.newAnnotation(ERROR, "Cannot use varargs in handler method")
                                .range(parameter)
                                .create();
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
                reasons.add("Handler Method should not have more than one parameter of type "
                        + ELASTIC_ACTOR_CLASS);
            }
            if (foundState > 1) {
                reasons.add("Handler Method should not have more than one parameter of type "
                        + ACTOR_STATE_CLASS);
            }
            if (foundActorSystem > 1) {
                reasons.add("Handler Method should not have more than one parameter of type "
                        + ACTOR_SYSTEM_CLASS);
            }
            return reasons;
        }

        return null;
    }

    private static boolean isActorState(@Nullable PsiClass paramClass) {
        return isInheritor(paramClass, ACTOR_STATE_CLASS);
    }

    private static boolean isActorSystem(@Nullable PsiClass paramClass) {
        return paramClass != null
                && Objects.equals(paramClass.getQualifiedName(), ACTOR_SYSTEM_CLASS);
    }

    private static boolean isActorRef(@Nullable PsiClass paramClass) {
        return paramClass != null
                && Objects.equals(paramClass.getQualifiedName(), ACTOR_REF_CLASS);
    }

}
