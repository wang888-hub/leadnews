package com.aaliyun.leadnews.wemedia.audit;

import com.aaliyun.leadnews.model.ai.*;import com.aaliyun.leadnews.wemedia.domain.*;import com.aaliyun.leadnews.wemedia.mapper.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;import org.junit.jupiter.api.*;import java.time.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;

class AuditTaskCoordinatorReliabilityTest {
 AuditTaskMapper tasks;WmNewsMapper news;WmNewsAuditRecordMapper records;AuditTaskCoordinator coordinator;
 WmNewsAuditRequestedEvent event=new WmNewsAuditRequestedEvent("E001",22L,5,Instant.now(),"t");
 @BeforeEach void setup(){
  TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"task"),AuditTask.class);
  TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"news"),WmNews.class);
  tasks=mock(AuditTaskMapper.class);news=mock(WmNewsMapper.class);records=mock(WmNewsAuditRecordMapper.class);
  coordinator=new AuditTaskCoordinator(tasks,news,records,new ObjectMapper());
 }
 private AuditTask task(int attempt,String status){AuditTask t=new AuditTask();t.setId(9L);t.setNewsId(22L);t.setAuditVersion(5L);t.setEventId("E001");t.setAttemptNo(attempt);t.setStatus(status);t.setDispatchStatus("SENT");t.setStartedAt(LocalDateTime.now().minusMinutes(2));return t;}
 private WmNews auditing(long version){WmNews n=new WmNews();n.setId(22L);n.setAuditVersion(version);n.setStatus(WmNewsStatus.AUDITING);return n;}

 @Test void duplicateKafkaDeliveryAndConcurrentClaimGrantOnlyOneExecution()throws Exception{
  AtomicInteger gate=new AtomicInteger();when(tasks.claim(eq("E001"),eq(22L),eq(5L),eq(3),any())).thenAnswer(i->gate.getAndIncrement()==0?1:0);
  when(tasks.findByEventId("E001")).thenReturn(task(1,"RUNNING"));when(news.selectById(22L)).thenReturn(auditing(5));
  ExecutorService pool=Executors.newFixedThreadPool(2);try{
   Callable<Optional<AuditTaskCoordinator.AuditExecution>> call=()->coordinator.claim(event,3);
   List<Future<Optional<AuditTaskCoordinator.AuditExecution>>> results=pool.invokeAll(List.of(call,call));
   int owners=0;for(Future<Optional<AuditTaskCoordinator.AuditExecution>> result:results)if(result.get().isPresent())owners++;
   assertThat(owners).isEqualTo(1);
  }finally{pool.shutdownNow();}
  verify(tasks,times(2)).claim(eq("E001"),eq(22L),eq(5L),eq(3),any());
 }

 @Test void claimReturnsIncrementedAttemptWithoutChangingAuditVersion(){
  when(tasks.claim(anyString(),anyLong(),anyLong(),anyInt(),any())).thenReturn(1);
  when(tasks.findByEventId("E001")).thenReturn(task(1,"RUNNING"),task(2,"RUNNING"));when(news.selectById(22L)).thenReturn(auditing(5));
  assertThat(coordinator.claim(event,3).orElseThrow().attemptNo()).isEqualTo(1);
  assertThat(coordinator.claim(event,3).orElseThrow().attemptNo()).isEqualTo(2);
  verify(tasks,times(2)).claim(eq("E001"),eq(22L),eq(5L),eq(3),any());
 }

 @Test void staleRunningRecoveryResetsDispatchButFreshTaskIsIgnored(){
  AuditTask zombie=task(1,"RUNNING");when(tasks.selectList(any())).thenReturn(List.of(zombie),List.of());when(tasks.retryAttempt(eq(9L),eq(1),any(),eq("RUNNING_TIMEOUT"))).thenReturn(1);
  assertThat(coordinator.recover(LocalDateTime.now().minusSeconds(90),3,50)).isEqualTo(1);
  assertThat(coordinator.recover(LocalDateTime.now().minusSeconds(90),3,50)).isZero();
  verify(tasks).retryAttempt(eq(9L),eq(1),any(),eq("RUNNING_TIMEOUT"));
 }

 @Test void oldAttemptResultIsRejectedBeforeArticleOrRecordWrites(){
  var old=new AuditTaskCoordinator.AuditExecution(event,9L,1);when(news.selectById(22L)).thenReturn(auditing(5));when(tasks.update(isNull(),any())).thenReturn(0);
  assertThat(coordinator.completeManual(old,"X","manual")).isFalse();verify(news,never()).update(isNull(),any());verifyNoInteractions(records);
 }

 @Test void currentAttemptAtomicallyCompletesTaskArticleAndRecord(){
  var current=new AuditTaskCoordinator.AuditExecution(event,9L,2);when(news.selectById(22L)).thenReturn(auditing(5));when(tasks.update(isNull(),any())).thenReturn(1);when(news.update(isNull(),any())).thenReturn(1);
  assertThat(coordinator.completeManual(current,"LOW_CONFIDENCE","manual")).isTrue();verify(tasks).update(isNull(),any());verify(news).update(isNull(),any());verify(records).insert(argThat((WmNewsAuditRecord r)->r.getAttemptNo()==2));
 }

 @Test void oldAuditVersionTerminatesStaleAndCannotTouchArticle(){
  var old=new AuditTaskCoordinator.AuditExecution(event,9L,1);when(news.selectById(22L)).thenReturn(auditing(6));when(tasks.update(isNull(),any())).thenReturn(1);
  assertThat(coordinator.completeManual(old,"X","old")).isFalse();verify(news,never()).update(isNull(),any());verifyNoInteractions(records);
 }

 @Test void finalAttemptFailureEndsTaskAndRoutesArticleToManual(){
  var last=new AuditTaskCoordinator.AuditExecution(event,9L,3);when(news.selectById(22L)).thenReturn(auditing(5));when(tasks.update(isNull(),any())).thenReturn(1);when(news.update(isNull(),any())).thenReturn(1);
  assertThat(coordinator.failAttempt(last,"AI_TIMEOUT","timeout",3,Duration.ofSeconds(5))).isTrue();
  verify(tasks,never()).retryAttempt(anyLong(),anyInt(),any(),anyString());verify(records).insert(argThat((WmNewsAuditRecord r)->"AI_TIMEOUT".equals(r.getErrorCode())&&r.getAttemptNo()==3));
 }

 @Test void articleFailureEscapesSoSpringTransactionRollsBackTaskCompletion(){
  var current=new AuditTaskCoordinator.AuditExecution(event,9L,2);when(news.selectById(22L)).thenReturn(auditing(5));when(tasks.update(isNull(),any())).thenReturn(1);when(news.update(isNull(),any())).thenThrow(new IllegalStateException("db"));
  assertThatThrownBy(()->coordinator.completeManual(current,"X","manual")).isInstanceOf(IllegalStateException.class);verifyNoInteractions(records);
 }
}
