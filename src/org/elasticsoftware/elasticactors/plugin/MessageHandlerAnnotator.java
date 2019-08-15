package org.elasticsoftware.elasticactors.plugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import org.elasticsoftware.elasticactors.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MessageHandlerAnnotator implements Annotator {

    @Override
    public void annotate(
            @NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof PsiMethod) {
            List<String> invalidReasons;
            PsiMethod psiMethod = (PsiMethod) element;
            if (psiMethod.hasAnnotation("org.elasticsoftware.elasticactors.MessageHandler")) {
                if (!psiMethod.hasModifier(JvmModifier.PUBLIC)) {
                    holder.createWarningAnnotation(
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
        }
    }
}
