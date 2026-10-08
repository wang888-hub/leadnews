package com.aaliyun.leadnews.article.mapper;
import com.aaliyun.leadnews.article.domain.ArticleSearchSync;import com.baomidou.mybatisplus.core.mapper.BaseMapper;import org.apache.ibatis.annotations.*;import java.time.LocalDateTime;
public interface ArticleSearchSyncMapper extends BaseMapper<ArticleSearchSync> {
 @Update("UPDATE article_search_sync SET status='PROCESSING',claimed_at=#{now},last_error=NULL WHERE id=#{id} AND status='PENDING' AND next_retry_time<=#{now}") int claim(@Param("id")long id,@Param("now")LocalDateTime now);
 @Update("UPDATE article_search_sync SET status='PENDING',claimed_at=NULL,next_retry_time=#{now},last_error='CLAIM_LEASE_TIMEOUT' WHERE status='PROCESSING' AND claimed_at<#{stale}") int recoverExpired(@Param("stale")LocalDateTime stale,@Param("now")LocalDateTime now);
 @Update("UPDATE article_search_sync SET status='SENT',claimed_at=NULL,last_error=NULL WHERE id=#{id} AND status='PROCESSING'") int markSent(@Param("id")long id);
 @Update("UPDATE article_search_sync SET status=#{status},retry_count=#{retry},claimed_at=NULL,next_retry_time=#{next},last_error=#{error} WHERE id=#{id} AND status='PROCESSING'") int markFailure(@Param("id")long id,@Param("status")String status,@Param("retry")int retry,@Param("next")LocalDateTime next,@Param("error")String error);
}
