package org.elasticsoftware.elasticactors.plugin;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

import static org.elasticsoftware.elasticactors.Utils.isHandler;

public class MessageHandlerImplicitUsageProvider implements ImplicitUsageProvider {

    @Override
    public boolean isImplicitUsage(PsiElement psiElement) {
        return psiElement instanceof PsiMethod && isHandler((PsiMethod) psiElement);
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
