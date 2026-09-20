package com.aaliyun.leadnews.gateway.filter;

import com.aaliyun.leadnews.common.trace.TraceConstants;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;

class TraceGlobalFilterTest {
    @Test void createsAndPropagatesTraceId() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/user/ping"));
        AtomicReference<String> downstream = new AtomicReference<>();
        new TraceGlobalFilter().filter(exchange, e -> { downstream.set(e.getRequest().getHeaders().getFirst(TraceConstants.TRACE_ID_HEADER)); return Mono.empty(); }).block();
        assertThat(downstream.get()).hasSize(32);
        assertThat(exchange.getResponse().getHeaders().getFirst(TraceConstants.TRACE_ID_HEADER)).isEqualTo(downstream.get());
    }
}
