package com.aaliyun.leadnews.wemedia.config;
import com.aaliyun.leadnews.wemedia.audit.SensitiveWordRegistry;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.*;
@Configuration
public class SensitiveWordRedisConfiguration {
 @Bean RedisMessageListenerContainer sensitiveWordListener(RedisConnectionFactory f,SensitiveWordRegistry r){var c=new RedisMessageListenerContainer();c.setConnectionFactory(f);c.addMessageListener(r,new ChannelTopic(SensitiveWordRegistry.CHANNEL));return c;}
}
