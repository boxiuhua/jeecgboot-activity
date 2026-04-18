package org.jeecg.modules.flowable.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Flowable 流程引擎自定义配置。
 * <p>
 * - 使用全局主数据源（dynamic-datasource master）建 ACT_* 表
 * - 保持与 JeecgBoot 的 MyBatis-Plus 独立的 SqlSessionFactory（Flowable 自带）
 * - 设置数据库 schema 更新策略，通过 yml 控制
 */
@Configuration
public class FlowableEngineConfig {

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> processEngineConfigurer() {
        return configuration -> {
            // 历史级别改为 full，便于追溯审批过程
            configuration.setHistoryLevel(org.flowable.common.engine.impl.history.HistoryLevel.FULL);
            // 启用任务相关事件审计
            configuration.setDbHistoryUsed(true);
            // 取消事务强绑定，兼容主数据源事务管理器
            configuration.setTransactionsExternallyManaged(false);
            // 自定义流程图字体，防止中文乱码
            configuration.setActivityFontName("宋体");
            configuration.setLabelFontName("宋体");
            configuration.setAnnotationFontName("宋体");
        };
    }

    /**
     * Flowable 7.x 的 SpringProcessEngineConfiguration 强制注入 @Qualifier("applicationTaskExecutor") AsyncTaskExecutor。
     * Spring Boot 的 TaskExecutionAutoConfiguration 带 @ConditionalOnMissingBean(Executor.class)，
     * 容器内只要已存在任何 Executor Bean（JeecgBoot 其他组件可能注册），该自动配置就会失效，
     * 导致 Flowable 启动阶段找不到 applicationTaskExecutor 而报错。这里兜底显式提供。
     */
    @Bean(name = "applicationTaskExecutor")
    @ConditionalOnMissingBean(name = "applicationTaskExecutor")
    public AsyncTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("flowable-task-");
        executor.initialize();
        return executor;
    }
}
