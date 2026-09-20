package com.aaliyun.leadnews.article.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "xxl.job.enabled", havingValue = "true")
public class XxlJobConfiguration {

    @Bean(initMethod = "start", destroyMethod = "destroy")
    XxlJobSpringExecutor xxl(
            @Value("${xxl.job.admin-addresses}") String admin,
            @Value("${xxl.job.executor.appname}") String app,
            @Value("${xxl.job.executor.address:}") String address,
            @Value("${xxl.job.executor.port:9999}") int port,
            @Value("${xxl.job.executor.log-path:./logs/xxl-job}") String logPath,
            @Value("${xxl.job.access-token:}") String token) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(admin);
        executor.setAppname(app);
        executor.setAddress(address);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setAccessToken(token);
        executor.setLogRetentionDays(7);
        return executor;
    }
}
