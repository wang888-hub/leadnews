package com.aaliyun.leadnews.model.foundation;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ScheduleTaskCommand(@NotNull Long articleId, @NotNull LocalDateTime executeTime) {}
