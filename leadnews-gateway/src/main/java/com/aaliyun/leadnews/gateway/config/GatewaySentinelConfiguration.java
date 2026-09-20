package com.aaliyun.leadnews.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.aaliyun.leadnews.common.api.CommonErrorCode;
import com.aaliyun.leadnews.common.sentinel.SentinelResources;
import com.aaliyun.leadnews.common.trace.TraceConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;

@Configuration
public class GatewaySentinelConfiguration {
    private static final Logger log = LoggerFactory.getLogger(GatewaySentinelConfiguration.class);

    public GatewaySentinelConfiguration(ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        GatewayCallbackManager.setBlockHandler(blockHandler(objectMapper, meterRegistry));
    }

    static BlockRequestHandler blockHandler(ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        return (exchange, throwable) -> {
            BlockException blocked = BlockException.isBlockException(throwable) ? (BlockException) throwable : null;
            String resource = blocked == null || blocked.getRule() == null ? "unknown" : blocked.getRule().getResource();
            String traceId = exchange.getRequest().getHeaders().getFirst(TraceConstants.TRACE_ID_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = exchange.getResponse().getHeaders().getFirst(TraceConstants.TRACE_ID_HEADER);
            }
            meterRegistry.counter(SentinelResources.METRIC_NAME, "resource", resource).increment();
            log.warn("Sentinel blocked gateway request resource={} ruleType={} traceId={}",
                    resource, throwable.getClass().getSimpleName(), traceId);
            return rateLimitedResponse(traceId);
        };
    }

    static Mono<ServerResponse> rateLimitedResponse(String traceId) {
        var body = new LinkedHashMap<String, Object>();
        body.put("code", CommonErrorCode.RATE_LIMITED.code());
        body.put("message", CommonErrorCode.RATE_LIMITED.message());
        body.put("data", null);
        body.put("timestamp", Instant.now().toEpochMilli());
        body.put("traceId", traceId == null ? "" : traceId);
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }
}
