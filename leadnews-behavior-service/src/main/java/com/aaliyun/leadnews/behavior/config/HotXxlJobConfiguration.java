package com.aaliyun.leadnews.behavior.config;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;import org.springframework.beans.factory.annotation.Value;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.context.annotation.*;
@Configuration @ConditionalOnProperty(name="xxl.job.enabled",havingValue="true") public class HotXxlJobConfiguration {
 @Bean(initMethod="start",destroyMethod="destroy") XxlJobSpringExecutor hotXxl(@Value("${xxl.job.admin-addresses}")String admin,@Value("${xxl.job.executor.appname}")String app,@Value("${xxl.job.executor.address:}")String address,@Value("${xxl.job.executor.port:9998}")int port,@Value("${xxl.job.executor.log-path:./logs/xxl-job-hot}")String logs,@Value("${xxl.job.access-token:}")String token){var e=new XxlJobSpringExecutor();e.setAdminAddresses(admin);e.setAppname(app);e.setAddress(address);e.setPort(port);e.setLogPath(logs);e.setAccessToken(token);e.setLogRetentionDays(7);return e;}
}
