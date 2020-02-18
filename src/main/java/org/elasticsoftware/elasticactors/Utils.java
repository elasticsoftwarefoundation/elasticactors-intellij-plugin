package org.elasticsoftware.elasticactors;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.intellij.psi.PsiModifier.ABSTRACT;
import static com.intellij.psi.util.InheritanceUtil.isInheritor;

import static java.util.Arrays.stream;

public final class Utils {

    public static final String ACTOR_STATE_CLASS = "org.elasticsoftware.elasticactors.ActorState";
    public static final String ACTOR_SYSTEM_CLASS = "org.elasticsoftware.elasticactors.ActorSystem";
    public static final String MESSAGE_ANNOTATION_CLASS =
            "org.elasticsoftware.elasticactors.serialization.Message";
    public static final String MESSAGE_HANDLER_ANNOTATION_CLASS =
            "org.elasticsoftware.elasticactors.MessageHandler";
    public static final String ACTOR_REF_CLASS = "org.elasticsoftware.elasticactors.ActorRef";
    public static final String ELASTIC_ACTOR_CLASS =
            "org.elasticsoftware.elasticactors.ElasticActor";

    private Utils() {
    }

    public static boolean isHandler(@NotNull PsiMethod psiMethod) {
        return psiMethod.hasAnnotation(MESSAGE_HANDLER_ANNOTATION_CLASS);
    }

    public static boolean isActorRefMethod(@NotNull PsiMethod method) {
        return stream(getDeepestSuperMethod(method))
                .map(PsiMethod::getContainingClass)
                .filter(Objects::nonNull)
                .map(PsiClass::getQualifiedName)
                .filter(Objects::nonNull)
                .anyMatch(fqn -> fqn.equals(ACTOR_REF_CLASS));
    }

    public static boolean isActorRef(@Nullable PsiClass containingClass) {
        return isInheritor(containingClass, ACTOR_REF_CLASS);
    }

    public static boolean isElasticActorMethod(@NotNull PsiMethod method) {
        return stream(getDeepestSuperMethod(method))
                .map(PsiMethod::getContainingClass)
                .filter(Objects::nonNull)
                .map(PsiClass::getQualifiedName)
                .filter(Objects::nonNull)
                .anyMatch(fqn -> fqn.equals(ELASTIC_ACTOR_CLASS));
    }

    public static boolean isElasticActor(@Nullable PsiClass containingClass) {
        return isInheritor(containingClass, ELASTIC_ACTOR_CLASS);
    }

    private static PsiMethod[] getDeepestSuperMethod(@NotNull PsiMethod method) {
        PsiMethod[] deepestSuperMethods = method.findDeepestSuperMethods();
        return deepestSuperMethods.length > 0 ? deepestSuperMethods : new PsiMethod[]{method};
    }

    public static boolean isMessage(@Nullable PsiClass argClass) {
        return argClass != null && argClass.hasAnnotation(MESSAGE_ANNOTATION_CLASS);
    }

    public static boolean isConcrete(@NotNull PsiClass argClass) {
        return !argClass.isAnnotationType()
                && !argClass.isInterface()
                && !argClass.hasModifierProperty(ABSTRACT);
    }
}
