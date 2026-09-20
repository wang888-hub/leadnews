package com.aaliyun.leadnews.model.foundation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ArticleContentItemDTO(
        @NotBlank @Pattern(regexp = "text|image") String type,
        @NotBlank @Size(max = 20000) String value) {}
