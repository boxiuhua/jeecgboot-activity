package org.jeecg.modules.flowable.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通知分发器：并行调用所有启用的 NotifyStrategy。
 */
@Slf4j
@Component
public class NotifyDispatcher {

    private final List<NotifyStrategy> strategies;

    public NotifyDispatcher(List<NotifyStrategy> strategies) {
        this.strategies = strategies;
    }

    public void dispatch(NotifyContext context) {
        if (context == null || context.getAssignee() == null || context.getAssignee().isEmpty()) {
            return;
        }
        for (NotifyStrategy s : strategies) {
            if (!s.isEnabled()) {
                continue;
            }
            try {
                s.send(context);
            } catch (Exception e) {
                log.warn("[flowable] 通知[{}]发送失败 assignee={} taskId={}: {}",
                        s.getType(), context.getAssignee(), context.getTaskId(), e.getMessage());
            }
        }
    }
}
