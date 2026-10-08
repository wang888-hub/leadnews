package com.aaliyun.leadnews.schedule.config;

import com.aaliyun.leadnews.common.persistence.AuditMetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(ScheduleProperties.class)
public class ScheduleConfiguration {

    @Bean
    MybatisPlusInterceptor interceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Bean
    AuditMetaObjectHandler audit() {
        return new AuditMetaObjectHandler();
    }

    @Bean("publishTaskExecutor")
    ThreadPoolTaskExecutor publishTaskExecutor(ScheduleProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.workerCoreSize());
        executor.setMaxPoolSize(properties.workerMaxSize());
        executor.setQueueCapacity(properties.workerQueueCapacity());
        executor.setThreadNamePrefix("article-publish-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // Rejection is handled by PublishTaskService, which puts the task back in ZSet.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
