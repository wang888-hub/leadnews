package com.aaliyun.leadnews.wemedia.mapper;

import com.aaliyun.leadnews.wemedia.domain.AuditTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

public interface AuditTaskMapper extends BaseMapper<AuditTask> {

 @Update("UPDATE wm_news_audit_task SET status='RUNNING',attempt_no=attempt_no+1,"+
         "dispatch_status='SENT',started_at=#{startedAt},finished_at=NULL,last_error=NULL " +
         "WHERE event_id=#{eventId} AND news_id=#{newsId} AND audit_version=#{auditVersion} " +
         "AND status='PENDING' AND attempt_no < #{maxAttempts}")
 int claim(@Param("eventId") String eventId,
           @Param("newsId") long newsId,
           @Param("auditVersion") long auditVersion,
           @Param("maxAttempts") int maxAttempts,
           @Param("startedAt") LocalDateTime startedAt);

 @Select("SELECT * FROM wm_news_audit_task WHERE event_id=#{eventId}")
 AuditTask findByEventId(@Param("eventId") String eventId);

 @Update("UPDATE wm_news_audit_task SET status='PENDING',dispatch_status='PENDING',"+
         "next_dispatch_time=#{nextDispatch},started_at=NULL,last_error=#{error} " +
         "WHERE id=#{id} AND status='RUNNING' AND attempt_no=#{attemptNo}")
 int retryAttempt(@Param("id") long id,
                  @Param("attemptNo") int attemptNo,
                  @Param("nextDispatch") LocalDateTime nextDispatch,
                  @Param("error") String error);

 @Update("UPDATE wm_news_audit_task SET status='PENDING',attempt_no=attempt_no-1,dispatch_status='PENDING',"+
         "next_dispatch_time=#{nextDispatch},started_at=NULL,last_error=#{error} " +
         "WHERE id=#{id} AND status='RUNNING' AND attempt_no=#{attemptNo}")
 int deferWithoutAttempt(@Param("id") long id,@Param("attemptNo") int attemptNo,
                         @Param("nextDispatch") LocalDateTime nextDispatch,@Param("error") String error);
}
