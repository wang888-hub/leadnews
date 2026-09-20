package com.aaliyun.leadnews.model.foundation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record CreateArticleCommand(@NotNull Long wmNewsId, @NotNull Long authorId, @NotBlank String authorName,
        @NotNull Long channelId, @NotBlank String title, @NotNull Integer layout,
        List<String> coverImages, String labels, @NotEmpty List<@Valid ArticleContentItemDTO> content,
        LocalDateTime publishTime) {}
