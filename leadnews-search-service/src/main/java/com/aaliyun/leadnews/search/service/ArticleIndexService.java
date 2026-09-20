package com.aaliyun.leadnews.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.aaliyun.leadnews.model.search.ArticleSearchDocument;
import com.aaliyun.leadnews.search.config.SearchIndexInitializer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class ArticleIndexService {

 private final ElasticsearchClient client;

 public ArticleIndexService(ElasticsearchClient c) {
  client = c;
 }

 public void upsert(ArticleSearchDocument d) throws IOException {
  client.index(i -> i
          .index(SearchIndexInitializer.ARTICLE_ALIAS)
          .id(d.articleId().toString())
          .document(d));

  Map<String, Object> s = new HashMap<>();
  s.put("articleId", d.articleId());
  s.put("title", d.title());
  s.put("suggestion", String.join(" ", combine(d.title(), d.labels())));
  s.put("labels", d.labels());

  client.index(i -> i
          .index(SearchIndexInitializer.SUGGEST_ALIAS)
          .id(d.articleId().toString())
          .document(s));
 }

 public void delete(Long id) throws IOException {
  try {
   client.delete(d -> d
           .index(SearchIndexInitializer.ARTICLE_ALIAS)
           .id(id.toString()));
  } catch (Exception ignored) {
  }
  try {
   client.delete(d -> d
           .index(SearchIndexInitializer.SUGGEST_ALIAS)
           .id(id.toString()));
  } catch (Exception ignored) {
  }
 }

 public void bulk(List<ArticleSearchDocument> docs) throws IOException {
  if (docs.isEmpty()) return;

  List<BulkOperation> ops = new ArrayList<>();
  for (ArticleSearchDocument d : docs) {
   ops.add(BulkOperation.of(o -> o
           .index(i -> i
                   .index(SearchIndexInitializer.ARTICLE_ALIAS)
                   .id(d.articleId().toString())
                   .document(d))));

   Map<String, Object> s = Map.of(
           "articleId", d.articleId(),
           "title", d.title(),
           "suggestion", String.join(" ", combine(d.title(), d.labels())),
           "labels", d.labels());

   ops.add(BulkOperation.of(o -> o
           .index(i -> i
                   .index(SearchIndexInitializer.SUGGEST_ALIAS)
                   .id(d.articleId().toString())
                   .document(s))));
  }

  BulkResponse r = client.bulk(b -> b.operations(ops));
  if (r.errors()) throw new IOException("Elasticsearch bulk contained failed items");
 }

 private List<String> combine(String title, List<String> labels) {
  List<String> all = new ArrayList<>();
  all.add(title);
  if (labels != null) all.addAll(labels);
  return all;
 }
}