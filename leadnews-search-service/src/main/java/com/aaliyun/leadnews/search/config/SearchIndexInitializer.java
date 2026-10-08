package com.aaliyun.leadnews.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;

@Component
public class SearchIndexInitializer {
    public static final String ARTICLE_INDEX = "leadnews_article_v1";
    public static final String ARTICLE_ALIAS = "leadnews_article";
    public static final String SUGGEST_INDEX = "leadnews_suggestion_v1";
    public static final String SUGGEST_ALIAS = "leadnews_suggestion";

    private final ElasticsearchClient client;

    public SearchIndexInitializer(ElasticsearchClient client) {
        this.client = client;
    }

    @PostConstruct
    public void initialize() throws IOException {
        initializeArticleIndex();
        initializeSuggestionIndex();
    }

    private void initializeArticleIndex() throws IOException {
        String mappings = """
                {
                  "dynamic": "strict",
                  "properties": {
                    "articleVersion": {"type": "long"},
                    "deleted": {"type": "boolean"},
                    "articleId": {"type": "long"},
                    "title": {"type": "text", "analyzer": "standard"},
                    "content": {"type": "text", "analyzer": "standard"},
                    "summary": {"type": "text", "analyzer": "standard"},
                    "authorId": {"type": "long"},
                    "authorName": {"type": "keyword"},
                    "channelId": {"type": "long"},
                    "channelName": {"type": "keyword"},
                    "labels": {"type": "keyword"},
                    "publishTime": {"type": "date"},
                    "staticUrl": {"type": "keyword", "index": false},
                    "createdTime": {"type": "date"}
                  }
                }
                """;
        if (!client.indices().exists(e -> e.index(ARTICLE_INDEX)).value()) {
            String definition = """
                    {
                      "settings": {"number_of_shards": 1, "number_of_replicas": 0},
                      "mappings": %s,
                      "aliases": {"leadnews_article": {}}
                    }
                    """.formatted(mappings);
            client.indices().create(c -> c.index(ARTICLE_INDEX).withJson(new StringReader(definition)));
        } else {
            client.indices().putMapping(m -> m.index(ARTICLE_INDEX).withJson(new StringReader(mappings)));
        }
    }

    private void initializeSuggestionIndex() throws IOException {
        String mappings = """
                {
                  "dynamic": "strict",
                  "properties": {
                    "articleVersion": {"type": "long"},
                    "deleted": {"type": "boolean"},
                    "articleId": {"type": "long"},
                    "title": {"type": "keyword"},
                    "suggestion": {
                      "type": "text",
                      "analyzer": "leadnews_suggest",
                      "search_analyzer": "standard"
                    },
                    "labels": {"type": "keyword"}
                  }
                }
                """;
        if (!client.indices().exists(e -> e.index(SUGGEST_INDEX)).value()) {
            String definition = """
                    {
                      "settings": {
                        "number_of_shards": 1,
                        "number_of_replicas": 0,
                        "analysis": {
                          "filter": {
                            "leadnews_edge": {"type": "edge_ngram", "min_gram": 1, "max_gram": 20}
                          },
                          "analyzer": {
                            "leadnews_suggest": {
                              "type": "custom",
                              "tokenizer": "standard",
                              "filter": ["lowercase", "leadnews_edge"]
                            }
                          }
                        }
                      },
                      "mappings": %s,
                      "aliases": {"leadnews_suggestion": {}}
                    }
                    """.formatted(mappings);
            client.indices().create(c -> c.index(SUGGEST_INDEX).withJson(new StringReader(definition)));
        } else {
            client.indices().putMapping(m -> m.index(SUGGEST_INDEX).withJson(new StringReader(mappings)));
        }
    }
}
