package org.elasticsoftware.elasticactors.plugin;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.elasticsoftware.elasticactors.Utils;

public class MessageHandlerUsageProvider implements ImplicitUsageProvider {

    @Override
    public boolean isImplicitUsage(PsiElement psiElement) {
        return psiElement instanceof PsiMethod
                && ((PsiMethod) psiElement).hasModifier(JvmModifier.PUBLIC)
                && ((PsiMethod) psiElement)
                .hasAnnotation("org.elasticsoftware.elasticactors.MessageHandler")
                && !Utils.isInvalidHandlerArguments(((PsiMethod) psiElement).getParameterList());
    }

    @Override
    public boolean isImplicitRead(PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean isImplicitWrite(PsiElement psiElement) {
        return false;
    }


}
