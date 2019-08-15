package org.elasticsoftware.elasticactors.plugin;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import com.intellij.usages.impl.rules.UsageTypeProviderEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.elasticsoftware.elasticactors.Utils.isActorAsk;
import static org.elasticsoftware.elasticactors.Utils.isActorRef;
import static org.elasticsoftware.elasticactors.Utils.isActorTell;
import static org.elasticsoftware.elasticactors.Utils.isHandler;

public class MessageHandlerUsageTypeProvider implements UsageTypeProvider {

    private static final UsageType MESSAGE_HANDLER =
            new UsageType("Elastic Actors message handler");
    private static final UsageType MESSAGE_SEND = new UsageType("Elastic Actors message sending");

    @Override
    public UsageType getUsageType(PsiElement element) {
        UsageType classUsageType = getClassUsageType(element);
        return classUsageType != null ? classUsageType : getMethodUsageType(element);
    }

    @Nullable
    private static UsageType getMethodUsageType(PsiElement element) {
        if (element instanceof PsiReferenceExpression) {
            PsiReferenceExpression referenceExpression = (PsiReferenceExpression) element;
            PsiElement callExpression = referenceExpression.getParent();
            if (callExpression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodCall = (PsiMethodCallExpression) callExpression;
                PsiMethod method = methodCall.resolveMethod();
                if (method != null
                        && ("tell".equals(method.getName()) || "ask".equals(method.getName()))
                        && isActorRef(method.getContainingClass())
                        && (isActorTell(method) || isActorAsk(method))) {
                    return MESSAGE_SEND;
                }
            }
        }
        return null;
    }


    @Nullable
    private static UsageType getClassUsageType(@NotNull PsiElement element) {

        PsiParameter psiParameter = PsiTreeUtil.getParentOfType(element, PsiParameter.class);
        if (psiParameter != null) {
            final PsiElement scope = psiParameter.getDeclarationScope();
            if (scope instanceof PsiMethod && isHandler((PsiMethod) scope)) {
                return MESSAGE_HANDLER;
            }
        }

        return null;
    }
}
