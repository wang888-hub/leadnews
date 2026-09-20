package com.aaliyun.leadnews.wemedia.config;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
@Validated @ConfigurationProperties("audit")
public record AuditProperties(boolean enabled,@NotNull Ai ai,@NotNull Kafka kafka,
 @NotNull Duration runningTimeout,@NotNull Duration recoveryScanInterval,@NotNull Duration taskRetryBackoff,
 @Min(1) int dispatchBatchSize,@Min(1) int maxAttempts){
 public record Ai(@NotBlank String model,@DecimalMin("0") @DecimalMax("1") double autoPassConfidenceThreshold,
                  @DecimalMin("0") @DecimalMax("1") double autoRejectConfidenceThreshold,
                  @Min(1) int maxImages,@Min(1) int maxTextLength){}
 public record Kafka(@NotBlank String topic,@NotBlank String dltTopic){}
}
