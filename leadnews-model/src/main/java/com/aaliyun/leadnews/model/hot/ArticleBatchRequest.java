package com.aaliyun.leadnews.model.hot;
import jakarta.validation.constraints.*;import java.util.List;
public record ArticleBatchRequest(@NotEmpty @Size(max=50) List<Long> articleIds) {}
