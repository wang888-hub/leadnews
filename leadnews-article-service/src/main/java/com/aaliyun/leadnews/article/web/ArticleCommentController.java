package com.aaliyun.leadnews.article.web;

import com.aaliyun.leadnews.article.service.ArticleCommentService;
import com.aaliyun.leadnews.common.api.ResponseResult;
import com.aaliyun.leadnews.model.foundation.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/article/{articleId}/comments")
public class ArticleCommentController {
    private final ArticleCommentService comments;

    public ArticleCommentController(ArticleCommentService comments) {
        this.comments = comments;
    }

    @GetMapping
    public ResponseResult<PageResponse<ArticleCommentService.CommentView>> list(
            @PathVariable long articleId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) long size) {
        return ResponseResult.success(comments.list(articleId, page, size));
    }

    @PostMapping
    public ResponseResult<ArticleCommentService.CommentView> create(
            @PathVariable long articleId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseResult.success(comments.create(articleId, request.content()));
    }

    public record CreateCommentRequest(@NotBlank @Size(max = 500) String content) {
    }
}
