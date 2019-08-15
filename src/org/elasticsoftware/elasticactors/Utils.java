package org.elasticsoftware.elasticactors;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.intellij.psi.util.InheritanceUtil.isInheritor;
import static com.intellij.psi.util.PsiTypesUtil.getParameterType;
import static com.intellij.psi.util.PsiTypesUtil.getPsiClass;
import static com.intellij.psi.util.PsiUtil.getMemberQualifiedName;

public final class Utils {

    private Utils() {
    }

    public static List<String> validateArguments(PsiParameterList psiParameterList) {
        int foundMessage = 0;
        int foundSender = 0;
        int foundState = 0;
        int foundActorSystem = 0;
        int foundVarArgs = 0;
        List<String> unknownClasses = new ArrayList<>();
        PsiParameter[] parameters = psiParameterList.getParameters();
        for (int i = 0; i < psiParameterList.getParametersCount(); i++) {
            PsiParameter parameter = parameters[i];
            if (parameter.isVarArgs()) {
                foundVarArgs++;
            }
            PsiClass paramClass =
                    getPsiClass(getParameterType(parameters, i, parameters[i].isVarArgs()));
            if (paramClass != null) {
                if (Objects.equals(
                        paramClass.getQualifiedName(),
                        "org.elasticsoftware.elasticactors.ActorRef")) {
                    foundSender++;
                } else if (Objects.equals(
                        paramClass.getQualifiedName(),
                        "org.elasticsoftware.elasticactors.ActorSystem")) {
                    foundActorSystem++;
                } else if (paramClass
                        .hasAnnotation("org.elasticsoftware.elasticactors.serialization.Message")) {
                    foundMessage++;
                } else if (isInheritor(
                        paramClass,
                        "org.elasticsoftware.elasticactors.ActorState")) {
                    foundState++;
                } else {
                    unknownClasses.add(paramClass.getQualifiedName());
                }
            }
        }
        if (psiParameterList.getParametersCount() == 0
                || foundMessage != 1
                || foundSender > 1
                || foundState > 1
                || foundActorSystem > 1
                || !unknownClasses.isEmpty()) {
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
                        "Handler Method should not have more than one parameter of type ActorRef");
            }
            if (foundState > 1) {
                reasons.add(
                        "Handler Method should not have more than one parameter of type "
                                + "ActorState");
            }
            if (foundActorSystem > 1) {
                reasons.add("Handler Method should not have more than one parameter of type "
                        + "ActorSystem");
            }
            if (!unknownClasses.isEmpty()) {
                reasons.add(
                        "Unexpected Parameter Types: [" + String.join(", ", unknownClasses) + "]");
            }
            if (foundVarArgs > 0) {
                reasons.add("Handler methods cannot use varArgs");
            }
            return reasons;
        }

        return null;
    }

    public static boolean isActorRef(PsiClass psiClass) {
        return isInheritor(psiClass, "org.elasticsoftware.elasticactors.ActorRef");
    }

    public static boolean isHandler(PsiMethod psiElement) {
        return psiElement.hasModifier(JvmModifier.PUBLIC)
                && psiElement.hasAnnotation("org.elasticsoftware.elasticactors.MessageHandler");
    }

    private static boolean isOverrideOf(@NotNull PsiMethod method, @NotNull String qualifiedName) {
        if (Objects.equals(getMemberQualifiedName(method), qualifiedName)) {
            return true;
        }
        PsiMethod[] superMethods = method.findDeepestSuperMethods();
        for (PsiMethod superMethod : superMethods) {
            if (Objects.equals(getMemberQualifiedName(superMethod), qualifiedName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isActorAsk(PsiMethod method) {
        return isOverrideOf(method, "org.elasticsoftware.elasticactors.ActorRef.ask");
    }

    public static boolean isActorTell(PsiMethod method) {
        return isOverrideOf(method, "org.elasticsoftware.elasticactors.ActorRef.tell");
    }
}
