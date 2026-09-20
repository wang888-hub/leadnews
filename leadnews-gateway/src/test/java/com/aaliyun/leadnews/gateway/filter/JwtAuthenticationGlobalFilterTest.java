package com.aaliyun.leadnews.gateway.filter;

import com.aaliyun.leadnews.common.security.JwtTokenService;
import com.aaliyun.leadnews.common.trace.TraceConstants;
import com.aaliyun.leadnews.gateway.config.GatewaySecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationGlobalFilterTest {
    private final JwtTokenService tokens = new JwtTokenService("stage3-test-secret-at-least-32-bytes-long");
    private final JwtAuthenticationGlobalFilter filter = new JwtAuthenticationGlobalFilter(tokens,
            new GatewaySecurityProperties("unused", List.of("/actuator/health")), new ObjectMapper());
    @Test void rejectsMissingToken() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/user/ping"));
        filter.filter(exchange, ignored -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test void overwritesForgedUserHeader() {
        String token = tokens.createToken(42, "APP", Duration.ofMinutes(5));
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/user/ping")
                .header("Authorization", "Bearer " + token).header(TraceConstants.USER_ID_HEADER, "1"));
        AtomicReference<String> downstream = new AtomicReference<>();
        GatewayFilterChain chain = e -> { downstream.set(e.getRequest().getHeaders().getFirst(TraceConstants.USER_ID_HEADER)); return Mono.empty(); };
        filter.filter(exchange, chain).block();
        assertThat(downstream).hasValue("42");
    }
    @Test void propagatesOnlyTokenUserType() {
        String token = tokens.createToken(9, "ADMIN", Duration.ofMinutes(5));
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/admin/channels")
                .header("Authorization", "Bearer " + token).header(TraceConstants.USER_TYPE_HEADER, "APP_USER"));
        AtomicReference<String> downstream = new AtomicReference<>();
        filter.filter(exchange, e -> { downstream.set(e.getRequest().getHeaders().getFirst(TraceConstants.USER_TYPE_HEADER)); return Mono.empty(); }).block();
        assertThat(downstream).hasValue("ADMIN");
    }
}
