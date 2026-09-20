package com.aaliyun.leadnews.model.hot;
import java.util.List;
public record HotArticleResponse(boolean degraded,List<HotArticleItem> items) {}
