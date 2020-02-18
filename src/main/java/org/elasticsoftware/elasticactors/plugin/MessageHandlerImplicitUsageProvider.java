package org.elasticsoftware.elasticactors.plugin;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import static com.intellij.psi.PsiModifier.PUBLIC;
import static org.elasticsoftware.elasticactors.Utils.isHandler;

public class MessageHandlerImplicitUsageProvider implements ImplicitUsageProvider {

    @Override
    public boolean isImplicitUsage(@NotNull PsiElement psiElement) {
        return psiElement instanceof PsiMethod
                && ((PsiMethod) psiElement).hasModifierProperty(PUBLIC)
                && isHandler((PsiMethod) psiElement);
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement psiElement) {
        return false;
    }


}
