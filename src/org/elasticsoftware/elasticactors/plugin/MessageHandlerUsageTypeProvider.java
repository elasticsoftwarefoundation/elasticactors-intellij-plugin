package org.elasticsoftware.elasticactors.plugin;

import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiInstanceOfExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiTypeCastExpression;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import org.jetbrains.annotations.NotNull;

import static com.intellij.psi.util.PsiTreeUtil.getParentOfType;
import static com.intellij.psi.util.PsiTreeUtil.isAncestor;
import static org.elasticsoftware.elasticactors.Utils.isActorRef;
import static org.elasticsoftware.elasticactors.Utils.isActorRefMethod;
import static org.elasticsoftware.elasticactors.Utils.isElasticActor;
import static org.elasticsoftware.elasticactors.Utils.isElasticActorMethod;
import static org.elasticsoftware.elasticactors.Utils.isHandler;

public class MessageHandlerUsageTypeProvider implements UsageTypeProvider {

    private static final String USAGE_PREFIX = "Actor message receive: ";

    private static final UsageType MESSAGE_HANDLER = new UsageType("Actor message handler");
    private static final UsageType MESSAGE_ASK = new UsageType("Actor message response type");
    private static final UsageType CLASS_CLASS_OBJECT_ACCESS =
            getUsageType(UsageType.CLASS_CLASS_OBJECT_ACCESS);
    private static final UsageType CLASS_CAST_TO = getUsageType(UsageType.CLASS_CAST_TO);
    private static final UsageType CLASS_INSTANCE_OF = getUsageType(UsageType.CLASS_INSTANCE_OF);

    @Override
    public UsageType getUsageType(PsiElement element) {

        PsiParameter psiParameter = getParentOfType(element, PsiParameter.class);
        if (psiParameter != null) {
            final PsiElement scope = psiParameter.getDeclarationScope();
            if (scope instanceof PsiMethod && isHandler((PsiMethod) scope)) {
                return MESSAGE_HANDLER;
            }
        }

        PsiClassObjectAccessExpression psiClassObjectAccess =
                getParentOfType(element, PsiClassObjectAccessExpression.class);
        if (psiClassObjectAccess != null) {
            PsiMethodCallExpression methodCall =
                    getParentOfType(psiClassObjectAccess, PsiMethodCallExpression.class);
            if (methodCall != null) {
                PsiElement nameElement = methodCall.getMethodExpression().getReferenceNameElement();
                if (nameElement != null && "ask".equals(nameElement.getText())) {
                    PsiMethod method = methodCall.resolveMethod();
                    if (method != null
                            && isActorRef(method.getContainingClass())
                            && isActorRefMethod(method)) {
                        return MESSAGE_ASK;
                    }
                }
            }
        }

        PsiMethod method = getParentOfType(element, PsiMethod.class);
        if (method != null
                && "onReceive".equals(method.getName())
                && isElasticActor(method.getContainingClass())
                && isElasticActorMethod(method)) {

            if (psiClassObjectAccess != null) {
                return CLASS_CLASS_OBJECT_ACCESS;
            }

            PsiTypeCastExpression castExpression =
                    getParentOfType(element, PsiTypeCastExpression.class);
            if (castExpression != null) {
                if (isAncestor(castExpression.getCastType(), element, true)) {
                    return CLASS_CAST_TO;
                }
            }

            PsiInstanceOfExpression instanceOfExpression =
                    getParentOfType(element, PsiInstanceOfExpression.class);
            if (instanceOfExpression != null) {
                if (isAncestor(instanceOfExpression.getCheckType(), element, true)) {
                    return CLASS_INSTANCE_OF;
                }
            }

        }

        return null;

    }

    @NotNull
    private static UsageType getUsageType(@NotNull UsageType usageType) {
        String usageString = usageType.toString();
        String description = usageString.substring(0, 1).toLowerCase() + usageString.substring(1);
        return new UsageType(USAGE_PREFIX + description);
    }

}
