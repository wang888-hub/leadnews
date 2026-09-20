package com.aaliyun.leadnews.wemedia.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("wm_news_audit_task")
public class AuditTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long newsId;
    private Long auditVersion;
    private String eventId;
    private String status;
    private Integer attemptNo;
    private String dispatchStatus;
    private Integer dispatchRetryCount;
    private String lastError;
    private LocalDateTime nextDispatchTime;
    private LocalDateTime lastDispatchedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long v) {
        newsId = v;
    }

    public Long getAuditVersion() {
        return auditVersion;
    }

    public void setAuditVersion(Long v) {
        auditVersion = v;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String v) {
        eventId = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer v) {
        attemptNo = v;
    }

    public String getDispatchStatus() {
        return dispatchStatus;
    }

    public void setDispatchStatus(String v) {
        dispatchStatus = v;
    }

    public Integer getDispatchRetryCount() {
        return dispatchRetryCount;
    }

    public void setDispatchRetryCount(Integer v) {
        dispatchRetryCount = v;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String v) {
        lastError = v;
    }

    public LocalDateTime getNextDispatchTime() {
        return nextDispatchTime;
    }

    public void setNextDispatchTime(LocalDateTime v) {
        nextDispatchTime = v;
    }

    public LocalDateTime getLastDispatchedAt() {
        return lastDispatchedAt;
    }

    public void setLastDispatchedAt(LocalDateTime v) {
        lastDispatchedAt = v;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime v) {
        startedAt = v;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime v) {
        finishedAt = v;
    }
}
