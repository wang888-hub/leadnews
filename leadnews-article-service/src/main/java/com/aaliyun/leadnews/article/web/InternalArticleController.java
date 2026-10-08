package com.aaliyun.leadnews.article.web;

import com.aaliyun.leadnews.article.service.*;
import com.aaliyun.leadnews.feign.article.ArticleInternalClient;
import com.aaliyun.leadnews.model.foundation.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/internal/articles")
public class InternalArticleController implements ArticleInternalClient {

 private final ArticleApplicationService articles;
 private final ArticlePublishService publisher;
 private final ChannelService channels;
 private final HotArticleQueryService hot;
 private final ArticleAiSummaryService summaries;
 private final ArticleSearchStateService searchState;

 public InternalArticleController(ArticleApplicationService a,
                                  ArticlePublishService p,
                                  ChannelService c,
                                  HotArticleQueryService h,
                                  ArticleAiSummaryService s,
                                  ArticleSearchStateService searchState) {
  articles = a;
  publisher = p;
  channels = c;
  hot = h;
  summaries = s;
  this.searchState = searchState;
 }

 @Override
 @PostMapping
 public ArticleCreatedResponse create(@Valid @RequestBody CreateArticleCommand c) {
  return articles.create(c);
 }

 @Override
 @PostMapping("/{id}/publish")
 public PublishResponse publish(@PathVariable Long id) {
  return publisher.publish(id);
 }

 @Override
 @GetMapping("/channels")
 public List<ChannelDTO> channels() {
  return channels.all();
 }

 @Override
 @PostMapping("/channels")
 public ChannelDTO createChannel(@Valid @RequestBody ChannelDTO c) {
  return channels.create(c);
 }

 @Override
 @PutMapping("/channels/{id}")
 public ChannelDTO updateChannel(@PathVariable Long id, @Valid @RequestBody ChannelDTO c) {
  return channels.update(id, c);
 }

 @Override
 @PostMapping("/channels/{id}/status")
 public ChannelDTO status(@PathVariable Long id, @RequestParam String status) {
  return channels.status(id, status);
 }

 @Override
 @DeleteMapping("/channels/{id}")
 public void deleteChannel(@PathVariable Long id) {
  channels.delete(id);
 }

 @Override
 @GetMapping("/{id}/search-document")
 public com.aaliyun.leadnews.model.search.ArticleSearchDocument searchDocument(@PathVariable Long id) {
  return articles.searchDocument(id);
 }

 @Override
 @GetMapping("/search-documents")
 public PageResponse<com.aaliyun.leadnews.model.search.ArticleSearchDocument> searchDocuments(
         @RequestParam long page,
         @RequestParam long size) {
  return articles.searchDocuments(page, size);
 }

 @Override
 @PostMapping("/batch")
 public List<com.aaliyun.leadnews.model.hot.HotArticleSummary> batch(
         @Valid @RequestBody com.aaliyun.leadnews.model.hot.ArticleBatchRequest r) {
  return hot.batch(r);
 }

 @Override
 @GetMapping("/hot-metadata")
 public PageResponse<com.aaliyun.leadnews.model.hot.HotArticleMetadata> hotMetadata(
         @RequestParam long page,
         @RequestParam long size,
         @RequestParam LocalDateTime publishedAfter) {
  return hot.metadata(page, size, publishedAfter);
 }

 @DeleteMapping("/{id}/hot-cache")
 public void cleanup(@PathVariable Long id) {
  hot.cleanup(id);
 }

 @PostMapping("/summary/backfill")
 public Map<String, Integer> backfill(
         @RequestParam(defaultValue = "1") long page,
         @RequestParam(defaultValue = "20") int size) {
  return Map.of("enqueued", summaries.backfillPage(page, Math.min(size, 100)));
 }

 @PostMapping("/{id}/summary/retry")
 public ArticleAiSummaryService.SummaryRetryResult retrySummary(@PathVariable Long id) {
  return summaries.retryFailed(id);
 }

 @PostMapping("/{id}/unpublish")
 public Map<String, Long> unpublish(@PathVariable Long id) {
  return Map.of("articleVersion", searchState.unpublish(id));
 }

 @PostMapping("/{id}/republish")
 public Map<String, Long> republish(@PathVariable Long id) {
  return Map.of("articleVersion", searchState.republish(id));
 }

 @DeleteMapping("/{id}")
 public Map<String, Long> deleteArticle(@PathVariable Long id) {
  return Map.of("articleVersion", searchState.delete(id));
 }
}
