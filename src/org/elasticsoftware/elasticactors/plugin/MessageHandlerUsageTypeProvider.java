package org.elasticsoftware.elasticactors.plugin;

import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;

import static com.intellij.psi.util.PsiTreeUtil.getParentOfType;
import static org.elasticsoftware.elasticactors.Utils.isActorAsk;
import static org.elasticsoftware.elasticactors.Utils.isActorRef;
import static org.elasticsoftware.elasticactors.Utils.isHandler;

public class MessageHandlerUsageTypeProvider implements UsageTypeProvider {

    private static final UsageType MESSAGE_HANDLER =
            new UsageType("Actor message handler");
    private static final UsageType MESSAGE_ASK = new UsageType("Actor message response type");

    @Override
    public UsageType getUsageType(PsiElement element) {
        PsiClassObjectAccessExpression psiClassObjectAccess =
                getParentOfType(element, PsiClassObjectAccessExpression.class);
        if (psiClassObjectAccess != null) {
            PsiMethodCallExpression methodCall =
                    getParentOfType(psiClassObjectAccess, PsiMethodCallExpression.class);
            if (methodCall != null) {
                PsiMethod method = methodCall.resolveMethod();
                if (method != null
                        && isActorRef(method.getContainingClass())
                        && isActorAsk(method)) {
                    return MESSAGE_ASK;
                }
            }
        }

        PsiParameter psiParameter = getParentOfType(element, PsiParameter.class);
        if (psiParameter != null) {
            final PsiElement scope = psiParameter.getDeclarationScope();
            if (scope instanceof PsiMethod && isHandler((PsiMethod) scope)) {
                return MESSAGE_HANDLER;
            }
        }

        return null;

    }

}
