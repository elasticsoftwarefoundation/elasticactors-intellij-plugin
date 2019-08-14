package org.elasticsoftware.elasticactors.plugin;

import com.intellij.lang.HelpID;
import com.intellij.lang.LangBundle;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiFormatUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MessageHandlerFindUsagesProvider implements FindUsagesProvider {

    @Nullable
    @Override
    public WordsScanner getWordsScanner() {
        return null;
    }

    @Override
    public boolean canFindUsagesFor(
            @NotNull PsiElement psiElement) {
        return psiElement instanceof PsiParameter;
    }

    @Nullable
    @Override
    public String getHelpId(@NotNull PsiElement psiElement) {
        return HelpID.FIND_OTHER_USAGES;
    }

    @NotNull
    @Override
    public String getType(@NotNull PsiElement psiElement) {
        return LangBundle.message("java.terms.parameter");
    }

    @NotNull
    @Override
    public String getDescriptiveName(@NotNull PsiElement psiElement) {
        if (psiElement instanceof PsiVariable) {
            return PsiFormatUtil.formatVariable((PsiVariable) psiElement, 1, PsiSubstitutor.EMPTY);
        }
        return "";
    }

    @NotNull
    @Override
    public String getNodeText(@NotNull PsiElement psiElement, boolean b) {
        if (psiElement instanceof PsiParameter
                && ((PsiParameter) psiElement).getDeclarationScope() instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) ((PsiParameter) psiElement).getDeclarationScope();
            if (method.hasModifier(JvmModifier.PUBLIC)
                    && method.hasAnnotation("org.elasticsoftware.elasticactors.MessageHandler")) {
                byte options = 7;
                int methodOptions = 257;
                String s = LangBundle.message(
                        "java.terms.variable.of.method",
                        PsiFormatUtil.formatVariable(
                                (PsiVariable) psiElement,
                                options,
                                PsiSubstitutor.EMPTY),
                        PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, methodOptions, 2));
                return appendClassName(s, method.getContainingClass());
            }
        }
        return "";
    }

    private static String appendClassName(String s, PsiClass psiClass) {
        if (psiClass != null) {
            String qName = psiClass.getQualifiedName();
            if (qName != null) {
                s = LangBundle.message(
                        psiClass.isInterface() ? "java.terms.of.interface" : "java.terms.of.class",
                        s,
                        qName);
            }
        }

        return s;
    }
}
