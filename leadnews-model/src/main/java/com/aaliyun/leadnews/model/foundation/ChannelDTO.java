package com.aaliyun.leadnews.model.foundation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ChannelDTO(Long id, @NotBlank @Size(max=64) String name, @Size(max=255) String description,
                         @NotNull String status, @NotNull @Min(0) Integer ord,
                         LocalDateTime createdTime, LocalDateTime updatedTime) {}
