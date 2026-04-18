package org.jeecg.modules.flowable.identity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时触发一次身份同步。
 * 通过 flowable.identity.sync-on-startup=false 可关闭。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JeecgIdentityInitRunner implements ApplicationRunner {

    private final JeecgIdentitySyncService syncService;

    @Value("${flowable.identity.sync-on-startup:true}")
    private boolean syncOnStartup;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (!syncOnStartup) {
            log.info("[flowable] 启动同步身份已关闭");
            return;
        }
        try {
            syncService.syncAll();
        } catch (Exception e) {
            log.warn("[flowable] 启动同步身份失败，稍后可通过管理页面触发: {}", e.getMessage());
        }
    }
}
