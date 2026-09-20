package com.aaliyun.leadnews.gateway.filter;

import com.aaliyun.leadnews.common.api.CommonErrorCode;
import com.aaliyun.leadnews.common.security.JwtTokenService;
import com.aaliyun.leadnews.common.trace.TraceConstants;
import com.aaliyun.leadnews.gateway.config.GatewaySecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {
    private final JwtTokenService tokens;
    private final GatewaySecurityProperties properties;
    private final ObjectMapper objectMapper;
    public JwtAuthenticationGlobalFilter(JwtTokenService tokens, GatewaySecurityProperties properties, ObjectMapper objectMapper) {
        this.tokens = tokens; this.properties = properties; this.objectMapper = objectMapper;
    }
    @Override public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/internal/")) return error(exchange, HttpStatus.NOT_FOUND, CommonErrorCode.AUTH_FORBIDDEN);
        var builder = exchange.getRequest().mutate().headers(h -> {
            h.remove(TraceConstants.USER_ID_HEADER); h.remove(TraceConstants.USER_TYPE_HEADER);
            h.remove(TraceConstants.INTERNAL_REQUEST_HEADER);
        });
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (isWhitelisted(path) && (authorization == null || authorization.isBlank())) return chain.filter(exchange.mutate().request(builder.build()).build());
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return error(exchange, HttpStatus.UNAUTHORIZED, CommonErrorCode.AUTH_UNAUTHORIZED);
        }
        try {
            var principal = tokens.parse(authorization.substring(7));
            builder.header(TraceConstants.USER_ID_HEADER, principal.userId().toString());
            builder.header(TraceConstants.USER_TYPE_HEADER, principal.clientType());
            return chain.filter(exchange.mutate().request(builder.build()).build());
        } catch (JwtException | IllegalArgumentException ex) {
            return error(exchange, HttpStatus.UNAUTHORIZED, CommonErrorCode.AUTH_UNAUTHORIZED);
        }
    }
    private boolean isWhitelisted(String path) {
        return properties.whitelist() != null && properties.whitelist().stream().anyMatch(rule -> rule.endsWith("/**") ? path.startsWith(rule.substring(0, rule.length() - 3)) : path.equals(rule));
    }
    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, CommonErrorCode code) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceConstants.TRACE_ID_HEADER);
        try {
            byte[] json = objectMapper.writeValueAsBytes(Map.of("code", code.code(), "message", code.message(),
                    "timestamp", Instant.now().toEpochMilli(), "traceId", traceId == null ? "" : traceId));
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(json);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception ex) {
            byte[] fallback = "{\"code\":50000,\"message\":\"Internal server error\"}".getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(fallback)));
        }
    }
    @Override public int getOrder() { return -100; }
}
