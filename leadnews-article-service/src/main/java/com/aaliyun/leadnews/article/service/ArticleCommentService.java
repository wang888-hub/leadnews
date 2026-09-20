package com.aaliyun.leadnews.article.service;

import com.aaliyun.leadnews.article.domain.Article;
import com.aaliyun.leadnews.article.domain.ArticleComment;
import com.aaliyun.leadnews.article.mapper.ArticleCommentMapper;
import com.aaliyun.leadnews.article.mapper.ArticleMapper;
import com.aaliyun.leadnews.common.api.CommonErrorCode;
import com.aaliyun.leadnews.common.context.UserContext;
import com.aaliyun.leadnews.common.exception.BusinessException;
import com.aaliyun.leadnews.model.foundation.PageResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ArticleCommentService {
    private final ArticleCommentMapper comments;
    private final ArticleMapper articles;

    public ArticleCommentService(ArticleCommentMapper comments, ArticleMapper articles) {
        this.comments = comments;
        this.articles = articles;
    }

    public PageResponse<CommentView> list(long articleId, long page, long size) {
        requirePublished(articleId);
        Page<ArticleComment> result = comments.selectPage(
                Page.of(Math.max(1, page), Math.min(50, Math.max(1, size))),
                new LambdaQueryWrapper<ArticleComment>()
                        .eq(ArticleComment::getArticleId, articleId)
                        .eq(ArticleComment::getStatus, "VISIBLE")
                        .eq(ArticleComment::getDeleted, false)
                        .orderByDesc(ArticleComment::getCreatedTime)
                        .orderByDesc(ArticleComment::getId));
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(),
                result.getRecords().stream().map(this::view).toList());
    }

    @Transactional
    public CommentView create(long articleId, String rawContent) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BusinessException(CommonErrorCode.AUTH_UNAUTHORIZED);
        if (!"APP_USER".equals(UserContext.getUserType())) {
            throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
        }
        requirePublished(articleId);
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty() || content.length() > 500) {
            throw new BusinessException(40000, "Comment must contain 1 to 500 characters");
        }
        LocalDateTime now = LocalDateTime.now();
        ArticleComment comment = new ArticleComment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setAuthorName("用户" + userId);
        comment.setContent(content);
        comment.setStatus("VISIBLE");
        comment.setCreatedTime(now);
        comment.setUpdatedTime(now);
        comment.setDeleted(false);
        comments.insert(comment);
        return view(comment);
    }

    private void requirePublished(long articleId) {
        Article article = articles.selectById(articleId);
        if (article == null || !"PUBLISHED".equals(article.getStatus()) || Boolean.TRUE.equals(article.getDeleted())) {
            throw new BusinessException(40400, "Article not found");
        }
    }

    private CommentView view(ArticleComment comment) {
        return new CommentView(comment.getId(), comment.getArticleId(), comment.getUserId(),
                comment.getAuthorName(), comment.getContent(), comment.getCreatedTime());
    }

    public record CommentView(Long id, Long articleId, Long userId, String authorName,
                              String content, LocalDateTime createdTime) {
    }
}
