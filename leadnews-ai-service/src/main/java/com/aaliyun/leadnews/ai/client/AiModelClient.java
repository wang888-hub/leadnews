package com.aaliyun.leadnews.ai.client;
import com.aaliyun.leadnews.ai.model.*;
import reactor.core.publisher.Flux;
public interface AiModelClient {
 AiCallResult chat(String systemPrompt,String userPrompt);
 <T> T structuredChat(String systemPrompt,String userPrompt,Class<T> type);
 <T> StructuredAiCallResult<T> structuredChatResult(String systemPrompt,String userPrompt,Class<T> type);
 <T> StructuredAiCallResult<T> structuredVision(String systemPrompt,String userPrompt,java.util.List<AiImageInput> images,Class<T> type);
 AiCallResult vision(String systemPrompt,String userPrompt,AiImageInput image);
 Flux<String> stream(String systemPrompt,String userPrompt);
}
