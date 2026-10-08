package com.aaliyun.leadnews.article.service;

import com.aaliyun.leadnews.article.domain.Article;
import com.aaliyun.leadnews.article.mapper.ArticleMapper;
import com.aaliyun.leadnews.model.search.ArticleSearchEvent;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ArticlePublicationFinalizer {

    private final ArticleMapper articles;
    private final ArticleSearchOutboxService outbox;

    public ArticlePublicationFinalizer(ArticleMapper a, ArticleSearchOutboxService o) {
        articles = a;
        outbox = o;
    }

    @Transactional
    public void complete(Long id, String key, String url, LocalDateTime done) {
        int n = articles.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .eq(Article::getPublishStatus, "PUBLISHING")
                .set(Article::getStaticObjectKey, key)
                .set(Article::getStaticUrl, url)
                .set(Article::getPublishStatus, "PUBLISHED")
                .set(Article::getStatus, "PUBLISHED")
                .set(Article::getPublishedTime, done)
                .set(Article::getLastError, null)
                .setSql("search_version = search_version + 1"));

        if (n != 1) {
            throw new IllegalStateException("Publish completion CAS failed");
        }

        Article published=articles.selectById(id);
        if(published==null)throw new IllegalStateException("Published article disappeared");
        outbox.enqueue(id, published.getSearchVersion(), ArticleSearchEvent.EventType.UPSERT);
    }
}
