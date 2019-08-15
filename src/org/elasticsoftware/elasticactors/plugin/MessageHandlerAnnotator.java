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
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.psi.util.PsiTypesUtil.getPsiClass;
import static com.intellij.psi.util.PsiUtil.resolveGenericsClassInType;
import static org.elasticsoftware.elasticactors.Utils.isActorAsk;
import static org.elasticsoftware.elasticactors.Utils.isActorRef;
import static org.elasticsoftware.elasticactors.Utils.isActorTell;
import static org.elasticsoftware.elasticactors.Utils.validateArguments;

public class MessageHandlerAnnotator implements Annotator {

    private static boolean isNotMessage(PsiClass argClass) {
        return argClass != null && !argClass.hasAnnotation(
                "org.elasticsoftware.elasticactors.serialization.Message");
    }

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
                if ((invalidReasons = validateArguments(psiMethod.getParameterList())) != null) {
                    invalidReasons.forEach(s -> holder.createErrorAnnotation(element, s));
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
                    && isActorRef(method.getContainingClass())) {
                if (isActorTell(method)) {
                    PsiClass argClass =
                            getPsiClass(methodCall.getArgumentList().getExpressionTypes()[0]);
                    if (isNotMessage(argClass)) {
                        holder.createWarningAnnotation(
                                element,
                                "Argument message should be of a type annotated with @Message");
                    }
                } else if (isActorAsk(method)) {
                    PsiClass argClass =
                            getPsiClass(methodCall.getArgumentList().getExpressionTypes()[0]);
                    if (isNotMessage(argClass)) {
                        holder.createWarningAnnotation(
                                element,
                                "Argument message should be of a type annotated with @Message");
                    }

                    ClassResolveResult resolveResult = resolveGenericsClassInType(
                            methodCall.getArgumentList().getExpressionTypes()[1]);
                    if (resolveResult.getElement() != null) {
                        PsiClass actualClass = getPsiClass(resolveResult.getSubstitutor()
                                .substitute(resolveResult.getElement().getTypeParameters()[0]));
                        if (isNotMessage(actualClass)) {
                            holder.createWarningAnnotation(
                                    element,
                                    "Argument responseType should refer to a type annotated with "
                                            + "@Message");
                        }
                    }
                }
            }
        }
    }

}
