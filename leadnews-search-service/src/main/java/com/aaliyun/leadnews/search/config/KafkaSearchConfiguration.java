package com.aaliyun.leadnews.search.config;

import co.elastic.clients.json.JsonpMappingException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;

@Configuration
public class KafkaSearchConfiguration {

    @Bean
    DefaultErrorHandler searchErrorHandler(
            KafkaTemplate<Object, Object> kafka,
            @Value("${leadnews.search.dlt-topic:leadnews.article.search.DLT}") String dltTopic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafka, (record, exception) -> new TopicPartition(dltTopic, record.partition()));
        ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(3);
        backoff.setInitialInterval(1_000);
        backoff.setMultiplier(2);
        backoff.setMaxInterval(5_000);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);
        // Malformed payloads and local JSON/mapping errors cannot become healthy by retrying.
        handler.addNotRetryableExceptions(DeserializationException.class, JsonpMappingException.class);
        return handler;
    }
}
