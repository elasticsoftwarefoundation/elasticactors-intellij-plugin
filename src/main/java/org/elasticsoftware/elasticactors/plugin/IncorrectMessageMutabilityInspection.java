package org.elasticsoftware.elasticactors.plugin;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.psi.PsiModifier.FINAL;
import static com.intellij.psi.PsiModifier.STATIC;
import static org.elasticsoftware.elasticactors.Utils.MESSAGE_ANNOTATION_CLASS;
import static org.elasticsoftware.elasticactors.Utils.isConcrete;
import static org.elasticsoftware.elasticactors.Utils.isMessage;

import static java.util.Arrays.stream;

public class IncorrectMessageMutabilityInspection extends AbstractBaseJavaLocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitClass(PsiClass aClass) {
                super.visitClass(aClass);
                if (isMessage(aClass) && isConcrete(aClass)) {
                    PsiIdentifier nameIdentifier = aClass.getNameIdentifier();
                    if (nameIdentifier != null) {
                        PsiAnnotation message = aClass.getAnnotation(MESSAGE_ANNOTATION_CLASS);
                        if (message != null) {
                            PsiAnnotationMemberValue immutable =
                                    message.findAttributeValue("immutable");
                            if (immutable != null) {
                                List<PsiField> nonStaticFields = stream(aClass.getAllFields())
                                        .filter(PsiElement::isValid)
                                        .filter(psiField -> !psiField.hasModifierProperty(STATIC))
                                        .filter(psiField -> psiField.getContainingClass() != null)
                                        .filter(psiField -> !Throwable.class.getName()
                                                .equals(psiField.getContainingClass()
                                                        .getQualifiedName()))
                                        .collect(Collectors.toList());
                                validateNonStaticFields(
                                        nameIdentifier,
                                        immutable,
                                        nonStaticFields,
                                        holder);
                            }
                        }
                    }
                }
            }
        };
    }

    private static void validateNonStaticFields(
            PsiIdentifier nameIdentifier,
            PsiAnnotationMemberValue immutable,
            List<PsiField> fields,
            @NotNull ProblemsHolder holder) {
        if (Boolean.parseBoolean(immutable.getText())) {
            if (!fields.isEmpty()
                    && fields.stream().anyMatch(psiField -> !psiField.hasModifierProperty(FINAL))) {
                holder.registerProblem(
                        nameIdentifier,
                        "@Message-annotated class marked as immutable has non-final fields");
            }
        } else {
            if (fields.isEmpty()
                    || fields.stream().allMatch(psiField -> psiField.hasModifierProperty(FINAL))) {
                holder.registerProblem(
                        nameIdentifier,
                        "@Message-annotated class marked as mutable doesn't have non-final fields");
            }
        }
    }

}
