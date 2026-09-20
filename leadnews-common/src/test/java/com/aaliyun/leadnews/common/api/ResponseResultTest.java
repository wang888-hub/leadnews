package com.aaliyun.leadnews.common.api;

import com.aaliyun.leadnews.common.trace.TraceContext;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResponseResultTest {
    @Test void includesCurrentTraceId() {
        TraceContext.setTraceId("trace-test");
        try { assertThat(ResponseResult.success("ok").traceId()).isEqualTo("trace-test"); }
        finally { TraceContext.clear(); }
    }
}
