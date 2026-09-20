package com.aaliyun.leadnews.wemedia.audit;
import com.aaliyun.leadnews.model.ai.WmNewsAuditRequestedEvent;import com.aaliyun.leadnews.wemedia.domain.*;import com.aaliyun.leadnews.wemedia.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;import com.baomidou.mybatisplus.core.MybatisConfiguration;import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;import org.junit.jupiter.api.Test;import java.time.*;import static org.assertj.core.api.Assertions.assertThat;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;
class AuditVersionSafetyTest {
 @Test void staleVersionClaimIsTerminatedWithoutAiOwnership(){
  TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"test"),AuditTask.class);
  AuditTaskMapper tasks=mock(AuditTaskMapper.class);WmNewsMapper news=mock(WmNewsMapper.class);WmNewsAuditRecordMapper records=mock(WmNewsAuditRecordMapper.class);
  var event=new WmNewsAuditRequestedEvent("old",1L,1,Instant.now(),null);AuditTask task=new AuditTask();task.setId(7L);task.setNewsId(1L);task.setAuditVersion(1L);task.setEventId("old");task.setAttemptNo(1);
  when(tasks.claim(eq("old"),eq(1L),eq(1L),eq(3),any())).thenReturn(1);when(tasks.findByEventId("old")).thenReturn(task);
  WmNews current=new WmNews();current.setAuditVersion(2L);current.setStatus(WmNewsStatus.AUDITING);when(news.selectById(1L)).thenReturn(current);when(tasks.update(isNull(),any())).thenReturn(1);
  var c=new AuditTaskCoordinator(tasks,news,records,new ObjectMapper());assertThat(c.claim(event,3)).isEmpty();verify(tasks).update(isNull(),any());
 }
}
