package org.elasticsoftware.elasticactors.plugin;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;

import static com.intellij.psi.util.PsiTypesUtil.getParameterType;
import static com.intellij.psi.util.PsiTypesUtil.getPsiClass;

public class MessageHandlerUsageProvider implements ImplicitUsageProvider {

    @Override
    public boolean isImplicitUsage(PsiElement psiElement) {
        return psiElement instanceof PsiMethod
                && ((PsiMethod) psiElement).hasModifier(JvmModifier.PUBLIC)
                && ((PsiMethod) psiElement)
                .hasAnnotation("org.elasticsoftware.elasticactors.MessageHandler")
                && hasMessageArgument(((PsiMethod) psiElement).getParameterList());
    }

    @Override
    public boolean isImplicitRead(PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean isImplicitWrite(PsiElement psiElement) {
        return false;
    }

    private static boolean hasMessageArgument(PsiParameterList psiParameterList) {
        PsiParameter[] parameters = psiParameterList.getParameters();
        for (int i = 0; i < psiParameterList.getParametersCount(); i++) {
            PsiClass paramClass =
                    getPsiClass(getParameterType(parameters, i, parameters[i].isVarArgs()));
            if (paramClass != null && paramClass
                    .hasAnnotation("org.elasticsoftware.elasticactors.serialization.Message")) {
                return true;
            }
        }
        return false;
    }
}
