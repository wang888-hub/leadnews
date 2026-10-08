package com.aaliyun.leadnews.search.messaging;

import com.aaliyun.leadnews.feign.article.ArticleInternalClient;
import com.aaliyun.leadnews.model.search.*;
import com.aaliyun.leadnews.search.service.ArticleIndexService;
import feign.FeignException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ArticleSearchListener {

 private final ArticleInternalClient articles;
 private final ArticleIndexService index;
 private final JdbcTemplate jdbc;

 public ArticleSearchListener(ArticleInternalClient a, ArticleIndexService i, JdbcTemplate j) {
  articles = a;
  index = i;
  jdbc = j;
 }

 @KafkaListener(
         topics = "${leadnews.search.topic:leadnews.article.search}",
         groupId = "leadnews-search-indexer")
 public void consume(ArticleSearchEvent event) throws Exception {
  if (event.eventType() == ArticleSearchEvent.EventType.DELETE) {
   index.tombstone(event.articleId(),event.articleVersion());
   return;
  }
  try {
   ArticleSearchDocument document=articles.searchDocument(event.articleId());
   if(document.articleVersion()<event.articleVersion())throw new IllegalStateException("Article projection is older than search event");
   if(document.articleVersion()==event.articleVersion())index.upsert(document);
  } catch (FeignException.NotFound e) {
   index.tombstone(event.articleId(),event.articleVersion());
  }
 }

 @KafkaListener(
         topics = "${leadnews.search.dlt-topic:leadnews.article.search.DLT}",
         groupId = "leadnews-search-dlt-audit")
 public void dlt(ArticleSearchEvent e) {
  jdbc.update(
          "INSERT INTO search_sync_failure(event_id,article_id,event_type,error_message) "
                  + "VALUES(?,?,?,?) "
                  + "ON DUPLICATE KEY UPDATE error_message=VALUES(error_message)",
          e.eventId(), e.articleId(), e.eventType().name(),
          "Moved to DLT after finite retries");
 }
}
