package org.jeecg.modules.flowable.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.dto.message.MessageDTO;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.springframework.stereotype.Component;

/**
 * 默认站内信通知，复用 JeecgBoot 现有消息能力。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteNotifyStrategy implements NotifyStrategy {

    private final ISysBaseAPI sysBaseAPI;

    @Override
    public String getType() {
        return "SITE";
    }

    @Override
    public void send(NotifyContext ctx) {
        try {
            String title = "【工作流】待办：" + ctx.getTaskName();
            StringBuilder body = new StringBuilder();
            if (ctx.getProcessName() != null) {
                body.append("流程：").append(ctx.getProcessName()).append("\n");
            }
            if (ctx.getBusinessTitle() != null && !"null".equals(ctx.getBusinessTitle())) {
                body.append("业务：").append(ctx.getBusinessTitle()).append("\n");
            }
            body.append("请登录系统的“我的待办”查看。");

            MessageDTO msg = new MessageDTO();
            msg.setFromUser("system");
            msg.setToUser(ctx.getAssignee());
            msg.setTitle(title);
            msg.setContent(body.toString());
            msg.setCategory("2");
            sysBaseAPI.sendSysAnnouncement(msg);
        } catch (Exception e) {
            log.warn("[flowable] 站内信发送失败: {}", e.getMessage());
        }
    }
}
