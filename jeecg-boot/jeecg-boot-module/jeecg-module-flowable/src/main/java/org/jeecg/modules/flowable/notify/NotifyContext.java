package org.jeecg.modules.flowable.notify;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotifyContext {
    /** 接收人（用户名） */
    private String assignee;
    private String taskId;
    private String taskName;
    private String processName;
    private String businessTitle;
    private String tenantId;
    /** 操作链接，由 NotifyStrategy 自行拼接 */
    private String actionUrl;
}
