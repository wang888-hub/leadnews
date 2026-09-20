package com.aaliyun.leadnews.wemedia.web;

import com.aaliyun.leadnews.common.exception.BusinessException;
import com.aaliyun.leadnews.wemedia.service.ArticleContinuationService;
import com.aaliyun.leadnews.wemedia.service.MaterialStorageService;
import com.aaliyun.leadnews.wemedia.service.WemediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ContinuationExceptionHandlerTest {
    private final ArticleContinuationService continuation = mock(ArticleContinuationService.class);
    private final WemediaController controller = new WemediaController(
            mock(WemediaService.class), mock(MaterialStorageService.class), continuation);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ContinuationExceptionHandler(new ObjectMapper())).build();

    @Test
    void targetLengthBelowMinimumReturnsParseable400SseAndDoesNotCallProvider() throws Exception {
        when(continuation.stream(eq(22L), anyString(), eq(80), anyString()))
                .thenThrow(new BusinessException(40000, "Continuation input exceeds limits"));

        mvc.perform(post("/api/wemedia/news/22/ai/continue")
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"instruction\":\"continue\",\"targetLength\":80,\"currentContent\":\"text\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event: error")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":40000")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"traceId\"")));
        verify(continuation, times(1)).stream(eq(22L), anyString(), eq(80), anyString());
    }

    @Test
    void malformedArticleIdReturns400InsteadOf500() throws Exception {
        mvc.perform(post("/api/wemedia/news/not-a-number/ai/continue")
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"instruction\":\"continue\",\"targetLength\":120,\"currentContent\":\"text\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":40000")));
        verifyNoInteractions(continuation);
    }

    @Test
    void authorizationAndStateErrorsKeepTheirHttpStatus() throws Exception {
        assertBusinessStatus(40100, 401);
        assertBusinessStatus(40300, 403);
        assertBusinessStatus(40900, 409);
    }

    @Test
    void validRequestKeepsMetaChunkDoneStreamingContract() {
        Flux<ServerSentEvent<Map<String, Object>>> stream = Flux.just(
                event("meta", Map.of("requestId", "r", "model", "qwen3.8-max")),
                event("chunk", Map.of("text", "part")),
                event("done", Map.of("finishReason", "STOP", "chunkCount", 1)));
        when(continuation.stream(22L, "continue", 120, "text")).thenReturn(stream);

        var events = controller.continueArticle(22L,
                new WemediaController.ContinuationRequest("continue", 120, "text")).collectList().block();
        assertThat(events).extracting(ServerSentEvent::event).containsExactly("meta", "chunk", "done");
    }

    @Test
    void aiUnavailableKeepsExistingSseErrorFrameSemantics() {
        when(continuation.stream(22L, "continue", 120, "text"))
                .thenReturn(Flux.just(event("error", Map.of("code", 50320,
                        "message", "AI continuation is temporarily unavailable"))));
        var events = controller.continueArticle(22L,
                new WemediaController.ContinuationRequest("continue", 120, "text")).collectList().block();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.event()).isEqualTo("error");
            assertThat(event.data()).containsEntry("code", 50320);
        });
    }

    private void assertBusinessStatus(int code, int expectedStatus) throws Exception {
        reset(continuation);
        when(continuation.stream(anyLong(), anyString(), anyInt(), anyString()))
                .thenThrow(new BusinessException(code, "expected"));
        mvc.perform(post("/api/wemedia/news/22/ai/continue")
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"instruction\":\"continue\",\"targetLength\":120,\"currentContent\":\"text\"}"))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":" + code)));
    }

    private ServerSentEvent<Map<String, Object>> event(String name, Map<String, Object> data) {
        return ServerSentEvent.<Map<String, Object>>builder().event(name).data(data).build();
    }
}
