package org.elasticsoftware.elasticactors.plugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType.ClassResolveResult;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import org.elasticsoftware.elasticactors.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static com.intellij.psi.util.InheritanceUtil.isInheritor;
import static com.intellij.psi.util.PsiTypesUtil.getPsiClass;
import static com.intellij.psi.util.PsiUtil.getMemberQualifiedName;
import static com.intellij.psi.util.PsiUtil.resolveGenericsClassInType;

public class MessageHandlerAnnotator implements Annotator {

    @Override
    public void annotate(
            @NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) element;
            List<String> invalidReasons;
            if (psiMethod.hasAnnotation("org.elasticsoftware.elasticactors.MessageHandler")) {
                if (!psiMethod.hasModifier(JvmModifier.PUBLIC)) {
                    holder.createErrorAnnotation(
                            element,
                            "Message Handler methods must be public");
                }
                if ((invalidReasons =
                        Utils.validateArguments(psiMethod.getParameterList())) != null) {
                    invalidReasons.forEach(s -> holder.createErrorAnnotation(
                            element,
                            String.join("\n", s)));
                }
                if (!PsiType.VOID.equals(psiMethod.getReturnType())) {
                    holder.createWarningAnnotation(
                            element,
                            "Message Handler methods should return void");
                }
            }
        } else if (element instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;
            PsiMethod method = methodCall.resolveMethod();
            if (method != null
                    && ("tell".equals(method.getName()) || "ask".equals(method.getName()))
                    && isInheritor(
                    method.getContainingClass(),
                    "org.elasticsoftware.elasticactors.ActorRef")) {
                if (isOverrideOf(method, "org.elasticsoftware.elasticactors.ActorRef.tell")) {
                    PsiClass argClass =
                            getPsiClass(methodCall.getArgumentList().getExpressionTypes()[0]);
                    if (argClass != null && !argClass.hasAnnotation(
                            "org.elasticsoftware.elasticactors.serialization.Message")) {
                        holder.createWarningAnnotation(
                                element,
                                "Argument message should be of a type annotated with @Message");
                    }
                } else if (isOverrideOf(method, "org.elasticsoftware.elasticactors.ActorRef.ask")) {
                    PsiClass argClass =
                            getPsiClass(methodCall.getArgumentList().getExpressionTypes()[0]);
                    if (argClass != null && !argClass.hasAnnotation(
                            "org.elasticsoftware.elasticactors.serialization.Message")) {
                        holder.createWarningAnnotation(
                                element,
                                "Argument message should be of a type annotated with @Message");
                    }

                    ClassResolveResult responseClass =
                            resolveGenericsClassInType(methodCall.getArgumentList()
                                    .getExpressionTypes()[1]);
                    if (responseClass.getElement() != null && !responseClass.getElement()
                            .hasAnnotation(
                                    "org.elasticsoftware.elasticactors.serialization.Message")) {
                        holder.createErrorAnnotation(
                                element,
                                "Argument responseType should refer to a type annotated with "
                                        + "@Message");
                    }
                }
            }
        }
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
}
