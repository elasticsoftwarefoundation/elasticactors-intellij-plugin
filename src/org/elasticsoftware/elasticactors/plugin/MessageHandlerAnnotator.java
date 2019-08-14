package org.elasticsoftware.elasticactors.plugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

public class MessageHandlerAnnotator implements Annotator {

    @Override
    public void annotate(
            @NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof PsiMethod && ((PsiMethod) element).hasAnnotation(
                "org.elasticsoftware.elasticactors.MessageHandler")) {
            if (!((PsiMethod) element).hasModifier(JvmModifier.PUBLIC)) {
                holder.createErrorAnnotation(element, "Message Handler methods must be public");
            } else if (((PsiMethod) element).hasModifier(JvmModifier.STATIC)) {
                holder.createErrorAnnotation(element, "Message Handler methods must not be static");
            }
        }
    }
}
