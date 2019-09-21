package org.elasticsoftware.elasticactors.plugin;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType.ClassResolveResult;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static com.intellij.psi.util.PsiTypesUtil.getPsiClass;
import static com.intellij.psi.util.PsiUtil.resolveGenericsClassInType;
import static org.elasticsoftware.elasticactors.Utils.isActorRef;
import static org.elasticsoftware.elasticactors.Utils.isActorRefMethod;
import static org.elasticsoftware.elasticactors.Utils.isConcrete;
import static org.elasticsoftware.elasticactors.Utils.isMessage;

public class SuspectActorRefCallsInspection extends AbstractBaseJavaLocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression methodCall) {
                super.visitMethodCallExpression(methodCall);
                PsiElement nameElement = methodCall.getMethodExpression().getReferenceNameElement();
                if (nameElement != null) {
                    String methodName = nameElement.getText();
                    boolean isTell = "tell".equals(methodName);
                    boolean isAsk = !isTell && "ask".equals(methodName);
                    if (isTell || isAsk) {
                        PsiMethod method = methodCall.resolveMethod();
                        if (method != null
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
        };
    }

    private static void validateMessageArgument(
            @NotNull ProblemsHolder holder,
            @NotNull PsiMethodCallExpression methodCall) {
        PsiExpression argument = methodCall.getArgumentList().getExpressions()[0];
        validateClass(
                holder,
                argument,
                argument.getType(),
                () -> getPsiClass(argument.getType()));
    }

    private static void validateResponseTypeArgument(
            @NotNull ProblemsHolder holder,
            @NotNull PsiMethodCallExpression methodCall) {
        PsiExpression argument = methodCall.getArgumentList().getExpressions()[1];
        ClassResolveResult resolveResult = resolveGenericsClassInType(argument.getType());
        if (resolveResult.getElement() != null) {
            validateClass(holder, argument, argument.getType(),
                    () -> getPsiClass(resolveResult.getSubstitutor()
                            .substitute(resolveResult.getElement()
                                    .getTypeParameters()[0])));
        }
    }

    private static void validateClass(
            @NotNull ProblemsHolder holder,
            @NotNull PsiExpression argument,
            @Nullable PsiType type,
            @NotNull Supplier<PsiClass> classSupplier) {
        PsiClass argClass = classSupplier.get();
        if (argClass == null) {
            registerProblemForType(holder, argument, type);
            return;
        }
        if (isJavaCorePackage(argClass)) {
            registerGenericProblem(holder, argument);
        } else if (isConcrete(argClass)) {
            if (isFinal(argClass)) {
                if (!isMessage(argClass)) {
                    registerGenericProblem(holder, argument);
                }
            } else {
                registerProblemForInheritors(holder, argument, argClass, false);
            }
        } else {
            registerProblemForInheritors(holder, argument, argClass, true);
        }
    }

    private static void registerProblemForInheritors(
            @NotNull ProblemsHolder holder,
            @NotNull PsiExpression argument,
            @NotNull PsiClass argClass,
            boolean registerIfHasNoInheritors) {
        List<PsiClass> foundClasses = new ArrayList<>(5);
        boolean[] found = {false};
        ClassInheritorsSearch.search(argClass).forEach(inheritor -> {
            found[0] = true;
            if (isConcrete(inheritor) && !isMessage(inheritor)) {
                foundClasses.add(inheritor);
            }
            return foundClasses.size() < 5;
        });
        if (foundClasses.isEmpty()) {
            if (registerIfHasNoInheritors && !found[0]) {
                registerGenericProblem(holder, argument);
            }
        } else {
            foundClasses
                    .forEach(inheritor -> registerProblemForInheritor(holder, argument, inheritor));
        }
    }

    private static void registerProblemForType(
            @NotNull ProblemsHolder holder,
            @NotNull PsiExpression argument,
            @Nullable PsiType type) {
        holder.registerProblem(argument, "Unexpected argument type: "
                + (type != null ? type.getCanonicalText() : "UNKNOWN"));
    }

    private static void registerGenericProblem(
            @NotNull ProblemsHolder holder,
            @NotNull PsiExpression argument) {
        holder.registerProblem(
                argument,
                "Argument should be of a type annotated with @Message");
    }

    private static void registerProblemForInheritor(
            @NotNull ProblemsHolder holder,
            @NotNull PsiExpression argument,
            @NotNull PsiClass psiClass) {
        holder.registerProblem(
                argument,
                "Found possible inheritor not annotated with @Message: "
                        + (psiClass instanceof PsiAnonymousClass
                        ? "Anonymous class in "
                        + getConcreteParentOfType(psiClass).getQualifiedName()
                        : psiClass.getQualifiedName()));
    }

    @NotNull
    private static PsiClass getConcreteParentOfType(@NotNull PsiClass psiClass) {
        PsiClass parent = psiClass;
        while (parent instanceof PsiAnonymousClass) {
            parent = PsiTreeUtil.getParentOfType(parent, PsiClass.class);
        }
        return parent != null ? parent : psiClass;
    }

    private static boolean isFinal(@NotNull PsiClass psiClass) {
        return psiClass.hasModifier(JvmModifier.FINAL) || !hasInheritableConstructors(psiClass);
    }

    private static boolean hasInheritableConstructors(@NotNull PsiClass psiClass) {
        PsiMethod[] constructors = psiClass.getConstructors();
        return constructors.length == 0
                || Arrays.stream(constructors).anyMatch(c -> !c.hasModifier(JvmModifier.PRIVATE));
    }

    private static boolean isJavaCorePackage(@NotNull PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();
        return qualifiedName == null || qualifiedName.startsWith("java.");
    }

}
