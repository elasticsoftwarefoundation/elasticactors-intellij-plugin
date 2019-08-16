package org.elasticsoftware.elasticactors.plugin;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType.ClassResolveResult;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static com.intellij.psi.util.PsiTypesUtil.getPsiClass;
import static com.intellij.psi.util.PsiUtil.resolveGenericsClassInType;
import static org.elasticsoftware.elasticactors.Utils.isActorRef;
import static org.elasticsoftware.elasticactors.Utils.isActorRefMethod;
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
            holder.registerProblem(
                    argument,
                    "Unexpected argument type: " +
                            (type != null
                                    ? type.getCanonicalText()
                                    : "UNKNOWN"));
            return;
        }
        if (isNotMessage(argClass)) {
            if (isConcrete(argClass)) {
                holder.registerProblem(
                        argument,
                        "Argument should be of a type annotated with @Message");
            }
            if (!isConcrete(argClass) || !argClass.hasModifier(JvmModifier.FINAL)) {
                ClassInheritorsSearch.search(argClass).findAll().stream()
                        .filter(SuspectActorRefCallsInspection::isConcrete)
                        .filter(SuspectActorRefCallsInspection::isNotMessage)
                        .forEach(psiClass -> holder.registerProblem(
                                argument,
                                "Found possible inheritor not annotated with @Message: "
                                        + psiClass.getQualifiedName()));
            }
        }
    }

    private static boolean isConcrete(@Nullable PsiClass argClass) {
        return argClass != null
                && !argClass.isInterface()
                && !argClass.hasModifier(JvmModifier.ABSTRACT);
    }

    private static boolean isNotMessage(@Nullable PsiClass argClass) {
        return !isMessage(argClass);
    }
}
