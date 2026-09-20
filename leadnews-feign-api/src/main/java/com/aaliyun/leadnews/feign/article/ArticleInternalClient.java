package com.aaliyun.leadnews.feign.article;

import com.aaliyun.leadnews.model.foundation.ArticleCreatedResponse;
import com.aaliyun.leadnews.model.foundation.ChannelDTO;
import com.aaliyun.leadnews.model.foundation.CreateArticleCommand;
import com.aaliyun.leadnews.model.foundation.PublishResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.aaliyun.leadnews.model.search.ArticleSearchDocument;
import com.aaliyun.leadnews.model.foundation.PageResponse;
import com.aaliyun.leadnews.model.hot.*;
import java.time.LocalDateTime;

@FeignClient(name="leadnews-article-service", path="/internal/articles")
public interface ArticleInternalClient {
    @PostMapping ArticleCreatedResponse create(@Valid @RequestBody CreateArticleCommand command);
    @PostMapping("/{id}/publish") PublishResponse publish(@PathVariable Long id);
    @GetMapping("/channels") List<ChannelDTO> channels();
    @PostMapping("/channels") ChannelDTO createChannel(@Valid @RequestBody ChannelDTO channel);
    @PutMapping("/channels/{id}") ChannelDTO updateChannel(@PathVariable Long id,@Valid @RequestBody ChannelDTO channel);
    @PostMapping("/channels/{id}/status") ChannelDTO status(@PathVariable Long id,@RequestParam String status);
    @DeleteMapping("/channels/{id}") void deleteChannel(@PathVariable Long id);
    @GetMapping("/{id}/search-document") ArticleSearchDocument searchDocument(@PathVariable Long id);
    @GetMapping("/search-documents") PageResponse<ArticleSearchDocument> searchDocuments(@RequestParam long page,@RequestParam long size);
    @PostMapping("/batch") List<HotArticleSummary> batch(@Valid @RequestBody ArticleBatchRequest request);
    @GetMapping("/hot-metadata") PageResponse<HotArticleMetadata> hotMetadata(@RequestParam long page,@RequestParam long size,@RequestParam LocalDateTime publishedAfter);
}
