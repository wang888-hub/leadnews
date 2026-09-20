package com.aaliyun.leadnews.model.foundation;

import java.util.List;
public record PageResponse<T>(long total, long page, long size, List<T> records) {}
