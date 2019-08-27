package org.elasticsoftware.elasticactors.plugin;

import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.usages.impl.rules.JavaUsageTypeProvider;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.psi.util.PsiTreeUtil.getParentOfType;
import static org.elasticsoftware.elasticactors.Utils.isActorRef;
import static org.elasticsoftware.elasticactors.Utils.isActorRefMethod;
import static org.elasticsoftware.elasticactors.Utils.isElasticActor;
import static org.elasticsoftware.elasticactors.Utils.isElasticActorMethod;
import static org.elasticsoftware.elasticactors.Utils.isHandler;

public class MessageHandlerUsageTypeProvider implements UsageTypeProvider {

    private static final UsageTypeProvider JAVA_USAGE_TYPE_PROVIDER = new JavaUsageTypeProvider();

    private static final Map<UsageType, UsageType> USAGES = new HashMap<>();

    private static final String USAGE_PREFIX = "Actor message receive (";
    private static final String USAGE_SUFFIX = ")";

    private static final UsageType MESSAGE_HANDLER = new UsageType("Actor message handler");
    private static final UsageType MESSAGE_ASK = new UsageType("Actor message response type");

    @Override
    public UsageType getUsageType(PsiElement element) {
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

        PsiParameter psiParameter = getParentOfType(element, PsiParameter.class);
        if (psiParameter != null) {
            final PsiElement scope = psiParameter.getDeclarationScope();
            if (scope instanceof PsiMethod && isHandler((PsiMethod) scope)) {
                return MESSAGE_HANDLER;
            }
        }

        PsiMethod method = getParentOfType(element, PsiMethod.class);
        if (method != null
                && "onReceive".equals(method.getName())
                && isElasticActor(method.getContainingClass())
                && isElasticActorMethod(method)) {
            UsageType javaUsageType = JAVA_USAGE_TYPE_PROVIDER.getUsageType(element);
            if (javaUsageType != null) {
                return actorUsage(javaUsageType);
            }
        }

        return null;

    }

    @NotNull
    private static UsageType actorUsage(@NotNull UsageType usageType) {
        return USAGES.computeIfAbsent(usageType, MessageHandlerUsageTypeProvider::getUsageType);
    }

    @NotNull
    private static UsageType getUsageType(@NotNull UsageType usageType) {
        return new UsageType(USAGE_PREFIX + usageType.toString() + USAGE_SUFFIX);
    }

}
