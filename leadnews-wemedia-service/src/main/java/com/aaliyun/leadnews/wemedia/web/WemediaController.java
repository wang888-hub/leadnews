package com.aaliyun.leadnews.wemedia.web;

import com.aaliyun.leadnews.common.api.ResponseResult;
import com.aaliyun.leadnews.model.foundation.*;
import com.aaliyun.leadnews.wemedia.domain.WmMaterial;
import com.aaliyun.leadnews.wemedia.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/wemedia")
public class WemediaController {

 private final WemediaService service;
 private final MaterialStorageService storage;
 private final ArticleContinuationService continuation;

 public WemediaController(WemediaService s,
                          MaterialStorageService ms,
                          ArticleContinuationService continuation) {
  service = s;
  storage = ms;
  this.continuation = continuation;
 }

 @PostMapping("/login")
 public ResponseResult<WemediaService.LoginResult> login(@Valid @RequestBody LoginRequest r) {
  return ResponseResult.success(service.login(r.name(), r.password()));
 }

 @PostMapping("/news")
 public ResponseResult<WmNewsSnapshot> create(@Valid @RequestBody NewsRequest r) {
  return ResponseResult.success(service.create(r.command()));
 }

 @PutMapping("/news/{id}")
 public ResponseResult<WmNewsSnapshot> update(@PathVariable Long id,
                                              @Valid @RequestBody NewsRequest r) {
  return ResponseResult.success(service.update(id, r.command()));
 }

 @GetMapping("/news/{id}")
 public ResponseResult<WmNewsSnapshot> detail(@PathVariable Long id) {
  return ResponseResult.success(service.detail(id));
 }

 @GetMapping("/news")
 public ResponseResult<PageResponse<WmNewsSnapshot>> list(
         @RequestParam(defaultValue = "1") @Min(1) long page,
         @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size) {
  return ResponseResult.success(service.list(page, size));
 }

 @DeleteMapping("/news/{id}")
 public ResponseResult<Void> delete(@PathVariable Long id) {
  service.delete(id);
  return ResponseResult.success(null);
 }

 @PostMapping("/news/{id}/submit")
 public ResponseResult<WmNewsSnapshot> submit(@PathVariable Long id) {
  return ResponseResult.success(service.submit(id));
 }

 @GetMapping("/materials")
 public ResponseResult<List<WmMaterial>> materials() {
  return ResponseResult.success(service.materialList());
 }

 @PostMapping(value = "/materials", consumes = "multipart/form-data")
 public ResponseResult<WmMaterial> upload(@RequestPart("file") MultipartFile file) {
  return ResponseResult.success(storage.upload(file));
 }

 @DeleteMapping("/materials/{id}")
 public ResponseResult<Void> deleteMaterial(@PathVariable Long id) {
  storage.delete(id);
  return ResponseResult.success(null);
 }

 @PostMapping(
         value = "/news/{id}/ai/continue",
         produces = MediaType.TEXT_EVENT_STREAM_VALUE
 )
 public Flux<ServerSentEvent<Map<String, Object>>> continueArticle(
         @PathVariable Long id,
         @Valid @RequestBody ContinuationRequest r) {
  return continuation.stream(
          id, r.instruction(), r.targetLength(), r.currentContent()
  );
 }

 public record LoginRequest(
         @NotBlank @Size(max = 64) String name,
         @NotBlank @Size(min = 6, max = 72) String password) {
 }

 public record NewsRequest(
         @NotBlank @Size(max = 128) String title,
         @NotEmpty List<@Valid ArticleContentItemDTO> content,
         @NotNull @Min(0) @Max(3) Integer layout,
         @NotNull Long channelId,
         @Size(max = 255) String labels,
         List<@Size(max = 500) String> coverImages,
         LocalDateTime publishTime,
         List<Long> materialIds) {

  WemediaService.NewsCommand command() {
   return new WemediaService.NewsCommand(
           title,
           content,
           layout,
           channelId,
           labels,
           coverImages,
           publishTime,
           materialIds
   );
  }
 }

 public record ContinuationRequest(
         @NotBlank String instruction,
         @NotNull @Positive Integer targetLength,
         @NotBlank String currentContent) {
 }
}