package com.aaliyun.leadnews.gateway.filter;

import com.aaliyun.leadnews.common.trace.TraceConstants;
import com.aaliyun.leadnews.common.trace.TraceContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceGlobalFilter implements GlobalFilter, Ordered {
    @Override public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(TraceConstants.TRACE_ID_HEADER);
        String traceId = incoming == null || incoming.isBlank() ? TraceContext.newTraceId() : incoming;
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> { h.remove(TraceConstants.TRACE_ID_HEADER); h.set(TraceConstants.TRACE_ID_HEADER, traceId); }))
                .build();
        mutated.getResponse().getHeaders().set(TraceConstants.TRACE_ID_HEADER, traceId);
        return chain.filter(mutated);
    }
    @Override public int getOrder() { return -200; }
}
