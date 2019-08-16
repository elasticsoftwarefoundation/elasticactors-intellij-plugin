package org.elasticsoftware.elasticactors;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import java.util.Objects;

import static com.intellij.psi.util.InheritanceUtil.isInheritor;

import static java.util.Arrays.stream;

public final class Utils {

    private Utils() {
    }

    public static boolean isHandler(PsiMethod psiMethod) {
        return psiMethod.hasAnnotation("org.elasticsoftware.elasticactors.MessageHandler");
    }

    public static boolean isActorRefMethod(PsiMethod method) {
        return stream(getDeepestSuperMethod(method))
                .map(PsiMethod::getContainingClass)
                .filter(Objects::nonNull)
                .map(PsiClass::getQualifiedName)
                .filter(Objects::nonNull)
                .anyMatch(fqn -> fqn.equals("org.elasticsoftware.elasticactors.ActorRef"));
    }

    public static boolean isActorRef(PsiClass containingClass) {
        return isInheritor(containingClass, "org.elasticsoftware.elasticactors.ActorRef");
    }

    private static PsiMethod[] getDeepestSuperMethod(PsiMethod method) {
        PsiMethod[] deepestSuperMethods = method.findDeepestSuperMethods();
        return deepestSuperMethods.length > 0 ? deepestSuperMethods : new PsiMethod[]{method};
    }

}
