package org.jeecg.modules.flowable.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.modules.flowable.common.FlowableConstants;
import org.springframework.stereotype.Component;

/**
 * 多租户上下文持有者。
 * <p>
 * 优先从线程本地变量读取（用于异步任务手动透传），其次从 HTTP 头 X-Tenant-Id 读取。
 */
@Component
public class JeecgTenantInfoHolder {

    private static final ThreadLocal<String> TENANT_HOLDER = new ThreadLocal<>();

    public String getCurrentTenantId() {
        String tenantId = TENANT_HOLDER.get();
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }
        try {
            HttpServletRequest request = SpringContextUtils.getHttpServletRequest();
            if (request != null) {
                String header = request.getHeader(CommonConstant.TENANT_ID);
                if (header != null && !header.isEmpty()) {
                    return header;
                }
            }
        } catch (Exception ignored) {
        }
        return FlowableConstants.DEFAULT_TENANT_ID;
    }

    public void setTenantId(String tenantId) {
        TENANT_HOLDER.set(tenantId);
    }

    public void clear() {
        TENANT_HOLDER.remove();
    }
}
