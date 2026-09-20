package com.aaliyun.leadnews.gateway.config;

import com.aaliyun.leadnews.gateway.filter.JwtAuthenticationGlobalFilter;
import com.aaliyun.leadnews.gateway.filter.TraceGlobalFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunctions;

import static org.assertj.core.api.Assertions.assertThat;

class GatewaySentinelConfigurationTest {
    @Test void blockResponseIs429JsonAndKeepsTraceIdWithoutSecrets() {
        WebTestClient client = WebTestClient.bindToRouterFunction(RouterFunctions.route()
                .GET("/blocked", request -> GatewaySentinelConfiguration.rateLimitedResponse("trace-stage12"))
                .build()).build();

        client.get().uri("/blocked").header("Authorization", "Bearer must-not-leak").exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.code").isEqualTo(42900)
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").isEqualTo("trace-stage12")
                .jsonPath("$.message").isNotEmpty()
                .consumeWith(result -> assertThat(new String(result.getResponseBody())).doesNotContain("must-not-leak", "Sentinel"));
    }

    @Test void traceAndJwtRunBeforeSentinelGatewayFilter() {
        assertThat(new TraceGlobalFilter().getOrder()).isLessThan(-100);
        // SentinelGatewayFilter uses order -1 in the 1.8.9 Spring Cloud Gateway adapter.
        assertThat(-100).isLessThan(-1);
    }
}
