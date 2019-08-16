package org.elasticsoftware.elasticactors;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.intellij.psi.util.InheritanceUtil.isInheritor;

import static java.util.Arrays.stream;

public final class Utils {

    private Utils() {
    }

    public static boolean isHandler(@NotNull PsiMethod psiMethod) {
        return psiMethod.hasAnnotation("org.elasticsoftware.elasticactors.MessageHandler");
    }

    public static boolean isActorRefMethod(@NotNull PsiMethod method) {
        return stream(getDeepestSuperMethod(method))
                .map(PsiMethod::getContainingClass)
                .filter(Objects::nonNull)
                .map(PsiClass::getQualifiedName)
                .filter(Objects::nonNull)
                .anyMatch(fqn -> fqn.equals("org.elasticsoftware.elasticactors.ActorRef"));
    }

    public static boolean isActorRef(@Nullable PsiClass containingClass) {
        return isInheritor(containingClass, "org.elasticsoftware.elasticactors.ActorRef");
    }

    private static PsiMethod[] getDeepestSuperMethod(@NotNull PsiMethod method) {
        PsiMethod[] deepestSuperMethods = method.findDeepestSuperMethods();
        return deepestSuperMethods.length > 0 ? deepestSuperMethods : new PsiMethod[]{method};
    }

    public static boolean isMessage(@Nullable PsiClass argClass) {
        return argClass != null && argClass.hasAnnotation(
                "org.elasticsoftware.elasticactors.serialization.Message");
    }
}
