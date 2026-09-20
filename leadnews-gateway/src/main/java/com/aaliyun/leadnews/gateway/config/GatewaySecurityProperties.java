package com.aaliyun.leadnews.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties("leadnews.gateway.security")
public record GatewaySecurityProperties(String jwtSecret, List<String> whitelist) {}
