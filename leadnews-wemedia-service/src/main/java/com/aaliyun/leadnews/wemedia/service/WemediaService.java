package com.aaliyun.leadnews.wemedia.service;

import com.aaliyun.leadnews.common.api.CommonErrorCode;
import com.aaliyun.leadnews.common.context.UserContext;
import com.aaliyun.leadnews.common.exception.BusinessException;
import com.aaliyun.leadnews.common.security.JwtTokenService;
import com.aaliyun.leadnews.model.foundation.*;
import com.aaliyun.leadnews.model.ai.AuditTrailItem;
import com.aaliyun.leadnews.wemedia.audit.AuditTaskCoordinator;
import com.aaliyun.leadnews.wemedia.domain.*;
import com.aaliyun.leadnews.wemedia.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class WemediaService {

 private final WmUserMapper users;
 private final WmNewsMapper news;
 private final WmMaterialMapper materials;
 private final WmNewsMaterialMapper links;
 private final AuditTaskMapper tasks;
 private final AuditTaskCoordinator audits;
 private final ObjectMapper json;
 private final BCryptPasswordEncoder passwords;
 private final JwtTokenService jwt;

 public WemediaService(WmUserMapper u,
                       WmNewsMapper n,
                       WmMaterialMapper m,
                       WmNewsMaterialMapper l,
                       AuditTaskMapper tasks,
                       AuditTaskCoordinator audits,
                       ObjectMapper j,
                       BCryptPasswordEncoder p,
                       JwtTokenService t) {
  users = u;
  news = n;
  materials = m;
  links = l;
  this.tasks = tasks;
  this.audits = audits;
  json = j;
  passwords = p;
  jwt = t;
 }

 public LoginResult login(String name, String password) {
  WmUser u = users.selectOne(new LambdaQueryWrapper<WmUser>()
          .eq(WmUser::getName, name)
          .eq(WmUser::getDeleted, false));
  if (u == null || !"ACTIVE".equals(u.getStatus()) || !passwords.matches(password, u.getPasswordHash())) {
   throw new BusinessException(CommonErrorCode.AUTH_UNAUTHORIZED);
  }
  long exp = 7200;
  return new LoginResult(
          jwt.createToken(u.getId(), "WEMEDIA", Duration.ofSeconds(exp)),
          exp,
          u.getId(),
          u.getNickname()
  );
 }

 @Transactional
 public WmNewsSnapshot create(NewsCommand c) {
  requireWemedia();
  WmNews n = new WmNews();
  n.setUserId(UserContext.getUserId());
  n.setStatus(WmNewsStatus.DRAFT);
  apply(n, c);
  news.insert(n);
  replaceLinks(n.getId(), c.materialIds());
  return snapshot(n);
 }

 @Transactional
 public WmNewsSnapshot update(Long id, NewsCommand c) {
  WmNews n = owned(id);
  if (!n.getStatus().editable()) {
   throw new BusinessException(40900, "Only DRAFT or REJECTED news can be edited");
  }
  apply(n, c);
  n.setReason(null);
  news.updateById(n);
  replaceLinks(id, c.materialIds());
  return snapshot(n);
 }

 public WmNewsSnapshot detail(Long id) {
  return snapshot(owned(id));
 }

 public PageResponse<WmNewsSnapshot> list(long page, long size) {
  requireWemedia();
  Page<WmNews> p = new Page<>(page, Math.min(size, 100));
  news.selectPage(p, new LambdaQueryWrapper<WmNews>()
          .eq(WmNews::getUserId, UserContext.getUserId())
          .eq(WmNews::getDeleted, false)
          .orderByDesc(WmNews::getCreatedTime));
  return new PageResponse<>(
          p.getTotal(),
          page,
          size,
          p.getRecords().stream().map(this::snapshot).toList()
  );
 }

 public void delete(Long id) {
  WmNews n = owned(id);
  if (!n.getStatus().editable()) {
   throw new BusinessException(40900, "Submitted news cannot be deleted");
  }
  news.deleteById(id);
 }

 @Transactional
 public WmNewsSnapshot submit(Long id) {
  WmNews n = owned(id);
  if (!n.getStatus().editable()) {
   throw new BusinessException(40900, "Illegal state transition");
  }
  int changed = news.update(null, new LambdaUpdateWrapper<WmNews>()
          .eq(WmNews::getId, id)
          .eq(WmNews::getStatus, n.getStatus())
          .set(WmNews::getStatus, WmNewsStatus.AUDITING)
          .set(WmNews::getSubmittedTime, LocalDateTime.now())
          .set(WmNews::getReason, null)
          .set(WmNews::getAuditSource, null)
          .setSql("audit_version = audit_version + 1"));
  if (changed != 1) {
   throw new BusinessException(40900, "Concurrent state update");
  }
  WmNews submitted = news.selectById(id);
  AuditTask task = new AuditTask();
  task.setNewsId(id);
  task.setAuditVersion(submitted.getAuditVersion());
  task.setEventId(UUID.randomUUID().toString());
  task.setStatus("PENDING");
  task.setAttemptNo(0);
  task.setDispatchStatus("PENDING");
  task.setDispatchRetryCount(0);
  task.setNextDispatchTime(LocalDateTime.now());
  tasks.insert(task);
  return snapshot(submitted);
 }

 public PageResponse<WmNewsSnapshot> pending(long page, long size) {
  requireInternal();
  Page<WmNews> p = new Page<>(page, Math.min(size, 100));
  news.selectPage(p, new LambdaQueryWrapper<WmNews>()
          .eq(WmNews::getStatus, WmNewsStatus.MANUAL_REVIEW)
          .eq(WmNews::getDeleted, false)
          .orderByAsc(WmNews::getSubmittedTime));
  return new PageResponse<>(
          p.getTotal(),
          page,
          size,
          p.getRecords().stream().map(this::snapshot).toList()
  );
 }

 public WmNewsSnapshot auditDetail(Long id) {
  requireInternal();
  WmNews n = require(id);
  if (n.getStatus() != WmNewsStatus.MANUAL_REVIEW
          && n.getStatus() != WmNewsStatus.APPROVED
          && n.getStatus() != WmNewsStatus.REJECTED) {
   throw new BusinessException(40900, "News is not auditable");
  }
  return snapshot(n);
 }

 @Transactional
 public WmNewsSnapshot approve(Long id) {
  requireInternal();
  WmNews n = require(id);
  if (Set.of(
          WmNewsStatus.APPROVED,
          WmNewsStatus.WAITING,
          WmNewsStatus.PUBLISHING,
          WmNewsStatus.PUBLISHED,
          WmNewsStatus.PUBLISH_FAILED
  ).contains(n.getStatus())) {
   return snapshot(n);
  }
  int changed = news.update(null, new LambdaUpdateWrapper<WmNews>()
          .eq(WmNews::getId, id)
          .eq(WmNews::getAuditVersion, n.getAuditVersion())
          .eq(WmNews::getStatus, WmNewsStatus.MANUAL_REVIEW)
          .set(WmNews::getStatus, WmNewsStatus.APPROVED)
          .set(WmNews::getAuditSource, "MANUAL")
          .set(WmNews::getReason, null));
  if (changed != 1) {
   throw new BusinessException(40900, "News state changed concurrently");
  }
  audits.manualRecord(id, n.getAuditVersion(), "APPROVE", "人工审核通过", UserContext.getUserId());
  return snapshot(news.selectById(id));
 }

 @Transactional
 public WmNewsSnapshot reject(Long id, String reason) {
  requireInternal();
  if (reason == null || reason.isBlank()) {
   throw new BusinessException(CommonErrorCode.VALIDATION);
  }
  WmNews n = require(id);
  int changed = news.update(null, new LambdaUpdateWrapper<WmNews>()
          .eq(WmNews::getId, id)
          .eq(WmNews::getAuditVersion, n.getAuditVersion())
          .eq(WmNews::getStatus, WmNewsStatus.MANUAL_REVIEW)
          .set(WmNews::getStatus, WmNewsStatus.REJECTED)
          .set(WmNews::getAuditSource, "MANUAL")
          .set(WmNews::getReason, reason));
  if (changed != 1) {
   throw new BusinessException(40900, "News state changed concurrently");
  }
  audits.manualRecord(id, n.getAuditVersion(), "REJECT", reason, UserContext.getUserId());
  return snapshot(news.selectById(id));
 }

 @Transactional
 public void publishStatus(Long id, String status, Long articleId) {
  if (!UserContext.isInternal()) {
   throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
  }
  WmNewsStatus target;
  try {
   target = WmNewsStatus.valueOf(status);
  } catch (Exception e) {
   throw new BusinessException(CommonErrorCode.VALIDATION);
  }
  if (!Set.of(
          WmNewsStatus.PUBLISHING,
          WmNewsStatus.PUBLISHED,
          WmNewsStatus.PUBLISH_FAILED
  ).contains(target)) {
   throw new BusinessException(CommonErrorCode.VALIDATION);
  }
  WmNews n = require(id);
  if (n.getStatus() == target) {
   return;
  }
  boolean allowed = (target == WmNewsStatus.PUBLISHING
          && Set.of(
          WmNewsStatus.APPROVED,
          WmNewsStatus.WAITING,
          WmNewsStatus.PUBLISH_FAILED,
          WmNewsStatus.PUBLISHING
  ).contains(n.getStatus()))
          || (target == WmNewsStatus.PUBLISHED && n.getStatus() == WmNewsStatus.PUBLISHING)
          || (target == WmNewsStatus.PUBLISH_FAILED && n.getStatus() == WmNewsStatus.PUBLISHING);
  if (!allowed) {
   throw new BusinessException(40900, "Illegal publish state transition");
  }
  news.update(null, new LambdaUpdateWrapper<WmNews>()
          .eq(WmNews::getId, id)
          .eq(WmNews::getStatus, n.getStatus())
          .set(WmNews::getStatus, target)
          .set(WmNews::getArticleId, articleId));
 }

 public List<WmMaterial> materialList() {
  requireWemedia();
  return materials.selectList(new LambdaQueryWrapper<WmMaterial>()
          .eq(WmMaterial::getUserId, UserContext.getUserId())
          .eq(WmMaterial::getDeleted, false));
 }

 public String continuationTitle(Long id) {
  requireWemedia();
  WmNews n = require(id);
  if (!UserContext.getUserId().equals(n.getUserId())) {
   throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
  }
  if (!Set.of(WmNewsStatus.DRAFT, WmNewsStatus.REJECTED).contains(n.getStatus())) {
   throw new BusinessException(40900, "AI continuation is only available for editable drafts");
  }
  return n.getTitle();
 }

 private void apply(WmNews n, NewsCommand c) {
  n.setTitle(c.title());
  n.setContent(write(c.content()));
  n.setLayout(c.layout());
  n.setChannelId(c.channelId());
  n.setLabels(c.labels());
  n.setCoverImages(write(c.coverImages() == null ? List.of() : c.coverImages()));
  n.setPublishTime(c.publishTime());
 }

 private void replaceLinks(Long newsId, List<Long> ids) {
  links.delete(new LambdaQueryWrapper<WmNewsMaterial>()
          .eq(WmNewsMaterial::getNewsId, newsId));
  if (ids != null) {
   for (int i = 0; i < ids.size(); i++) {
    Long materialId = ids.get(i);
    WmMaterial m = materials.selectById(materialId);
    if (m == null || !UserContext.getUserId().equals(m.getUserId())) {
     throw new BusinessException(40300, "Material ownership denied");
    }
    WmNewsMaterial l = new WmNewsMaterial();
    l.setNewsId(newsId);
    l.setMaterialId(materialId);
    l.setReferenceType("CONTENT");
    l.setOrd(i);
    links.insert(l);
   }
  }
 }

 private WmNews owned(Long id) {
  requireWemedia();
  WmNews n = require(id);
  if (!UserContext.getUserId().equals(n.getUserId())) {
   throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
  }
  return n;
 }

 private WmNews require(Long id) {
  WmNews n = news.selectById(id);
  if (n == null) {
   throw new BusinessException(40400, "News not found");
  }
  return n;
 }

 private void requireWemedia() {
  if (!UserContext.isType("WEMEDIA")) {
   throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
  }
 }

 private void requireInternal() {
  if (!UserContext.isInternal() || !UserContext.isType("ADMIN")) {
   throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
  }
 }

 private WmNewsSnapshot snapshot(WmNews n) {
  WmUser u = users.selectById(n.getUserId());
  List<AuditTrailItem> trail = audits.trail(n.getId()).stream()
          .map(r -> new AuditTrailItem(
                  r.getAuditVersion(),
                  r.getAuditStage(),
                  r.getDecision(),
                  r.getRiskLevel(),
                  r.getReason(),
                  r.getConfidence(),
                  read(r.getRiskTags(), new TypeReference<List<String>>() {}),
                  r.getModel(),
                  r.getErrorCode(),
                  r.getReviewerId(),
                  r.getCreatedTime()))
          .toList();
  return new WmNewsSnapshot(
          n.getId(),
          n.getUserId(),
          u == null ? "unknown" : u.getNickname(),
          n.getTitle(),
          read(n.getContent(), new TypeReference<List<ArticleContentItemDTO>>() {}),
          n.getLayout(),
          n.getChannelId(),
          n.getLabels(),
          read(n.getCoverImages(), new TypeReference<List<String>>() {}),
          n.getStatus().name(),
          n.getSubmittedTime(),
          n.getPublishTime(),
          n.getReason(),
          n.getAuditVersion(),
          n.getAuditSource(),
          trail
  );
 }

 private String write(Object o) {
  try {
   return json.writeValueAsString(o);
  } catch (Exception e) {
   throw new BusinessException(40000, "Invalid JSON content");
  }
 }

 private <T> T read(String s, TypeReference<T> t) {
  try {
   return json.readValue(s, t);
  } catch (Exception e) {
   throw new BusinessException(50000, "Stored JSON is invalid");
  }
 }

 public record LoginResult(String token, long expiresIn, Long userId, String nickname) {
 }

 public record NewsCommand(
         String title,
         List<ArticleContentItemDTO> content,
         Integer layout,
         Long channelId,
         String labels,
         List<String> coverImages,
         LocalDateTime publishTime,
         List<Long> materialIds) {
 }
}
