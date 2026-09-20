package com.aaliyun.leadnews.gateway.config;

import com.aaliyun.leadnews.common.security.JwtTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewayConfiguration {
    @Bean JwtTokenService jwtTokenService(GatewaySecurityProperties properties) {
        return new JwtTokenService(properties.jwtSecret());
    }
}
