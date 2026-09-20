package com.aaliyun.leadnews.article.service;

import com.aaliyun.leadnews.article.config.PublishProperties;
import com.aaliyun.leadnews.article.domain.*;
import com.aaliyun.leadnews.article.mapper.*;
import com.aaliyun.leadnews.common.exception.BusinessException;
import com.aaliyun.leadnews.feign.wemedia.WemediaAuditClient;
import com.aaliyun.leadnews.model.foundation.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class ArticlePublishService {

 private static final Logger log = LoggerFactory.getLogger(ArticlePublishService.class);

 private final ArticleMapper articles;
 private final ArticleContentMapper contents;
 private final ObjectMapper json;
 private final ArticleHtmlRenderer renderer;
 private final ArticleObjectStorage storage;
 private final PublishProperties props;
 private final WemediaAuditClient wemedia;
 private final ArticlePublicationFinalizer finalizer;
 private final ArticleAiSummaryService summaries;
 private final ArticleHotMetadataService hotMetadata;

 public ArticlePublishService(ArticleMapper a,
                              ArticleContentMapper c,
                              ObjectMapper j,
                              ArticleHtmlRenderer r,
                              ArticleObjectStorage s,
                              PublishProperties p,
                              WemediaAuditClient w,
                              ArticlePublicationFinalizer f,
                              ArticleHotMetadataService h,
                              ArticleAiSummaryService summaries) {
  articles = a;
  contents = c;
  json = j;
  renderer = r;
  storage = s;
  props = p;
  wemedia = w;
  finalizer = f;
  hotMetadata = h;
  this.summaries = summaries;
 }

 public PublishResponse publish(Long id) {
  Article current = require(id);
  if (ArticlePublishStatus.PUBLISHED.name().equals(current.getPublishStatus())) {
   return response(current, true);
  }
  if (current.getPublishTime() != null && current.getPublishTime().isAfter(LocalDateTime.now())) {
   return response(current, false);
  }

  LocalDateTime now = LocalDateTime.now();
  LocalDateTime stale = now.minus(props.timeout());

  LambdaUpdateWrapper<Article> claim = new LambdaUpdateWrapper<Article>()
          .eq(Article::getId, id)
          .and(w -> w
                  .in(Article::getPublishStatus, List.of("APPROVED", "WAITING", "PUBLISH_FAILED"))
                  .or(x -> x
                          .eq(Article::getPublishStatus, "PUBLISHING")
                          .lt(Article::getPublishStartedTime, stale)))
          .lt(Article::getPublishRetryCount, props.maxRetries())
          .set(Article::getPublishStatus, "PUBLISHING")
          .set(Article::getPublishStartedTime, now)
          .set(Article::getLastError, null);

  if (articles.update(null, claim) != 1) {
   Article raced = require(id);
   if ("PUBLISHED".equals(raced.getPublishStatus())) {
    return response(raced, true);
   }
   throw new BusinessException(40900, "Article publish is already running or retries exhausted");
  }

  try {
   Article a = require(id);
   wemedia.publishStatus(a.getWmNewsId(), "PUBLISHING", id);

   ArticleContent c = contents.selectOne(
           new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, id));
   List<ArticleContentItemDTO> items = json.readValue(c.getContent(), new TypeReference<>() {});

   String html = renderer.render(a, items);
   String key = storage.key(id);
   storage.putHtml(key, html);

   finalizer.complete(id, key, storage.publicUrl(key), LocalDateTime.now());

   Article published = require(id);
   hotMetadata.putBestEffort(published);
   wemedia.publishStatus(a.getWmNewsId(), "PUBLISHED", id);

   try {
    summaries.enqueuePublished(id);
   } catch (Exception summaryError) {
    log.warn("Summary task enqueue deferred to backfill articleId={} errorCode={}",
            id, summaryError.getClass().getSimpleName());
   }

   return response(published, false);

  } catch (Exception e) {
   String msg = safe(e);
   Article failed = require(id);
   articles.update(null, new LambdaUpdateWrapper<Article>()
           .eq(Article::getId, id)
           .eq(Article::getPublishStatus, "PUBLISHING")
           .setSql("publish_retry_count = publish_retry_count + 1")
           .set(Article::getPublishStatus, "PUBLISH_FAILED")
           .set(Article::getLastError, msg));

   try {
    wemedia.publishStatus(failed.getWmNewsId(), "PUBLISH_FAILED", id);
   } catch (Exception sync) {
    log.warn("Wemedia failure status sync failed articleId={}", id, sync);
   }

   log.error("Article publish failed, articleId={}", id, e);
   throw new BusinessException(50300, "Article publish failed and can be retried");
  }
 }

 public int compensate() {
  LocalDateTime stale = LocalDateTime.now().minus(props.timeout());
  List<Article> candidates = articles.selectList(
          new LambdaQueryWrapper<Article>()
                  .and(w -> w
                          .eq(Article::getPublishStatus, "PUBLISH_FAILED")
                          .or(x -> x
                                  .eq(Article::getPublishStatus, "PUBLISHING")
                                  .lt(Article::getPublishStartedTime, stale)))
                  .lt(Article::getPublishRetryCount, props.maxRetries())
                  .last("LIMIT 100"));

  int ok = 0;
  for (Article a : candidates) {
   try {
    publish(a.getId());
    ok++;
   } catch (Exception ignored) {
   }
  }
  return ok;
 }

 private Article require(Long id) {
  Article a = articles.selectById(id);
  if (a == null) throw new BusinessException(40400, "Article not found");
  return a;
 }

 private PublishResponse response(Article a, boolean already) {
  return new PublishResponse(
          a.getId(),
          a.getPublishStatus(),
          a.getStaticObjectKey(),
          a.getStaticUrl(),
          already);
 }

 private String safe(Exception e) {
  String m = e.getMessage();
  if (m == null) m = e.getClass().getSimpleName();
  return m.length() > 500 ? m.substring(0, 500) : m;
 }
}