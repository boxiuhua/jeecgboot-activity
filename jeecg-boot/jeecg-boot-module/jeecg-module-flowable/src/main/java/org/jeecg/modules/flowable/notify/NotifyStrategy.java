package org.jeecg.modules.flowable.notify;

/**
 * 通知策略扩展点。通过 Spring 自动发现多实现，外部 IM 只需实现此接口并注入为 Bean。
 */
public interface NotifyStrategy {

    /** 策略类型：SITE(站内)/DINGTALK/WECOM/EMAIL ... */
    String getType();

    /** 是否启用（由配置或运行时判断） */
    default boolean isEnabled() {
        return true;
    }

    void send(NotifyContext context);
}
