package com.aaliyun.leadnews.wemedia.service;
import com.aaliyun.leadnews.common.context.UserContext;import com.aaliyun.leadnews.common.security.JwtTokenService;import com.aaliyun.leadnews.wemedia.audit.AuditTaskCoordinator;
import com.aaliyun.leadnews.wemedia.domain.*;import com.aaliyun.leadnews.wemedia.mapper.*;import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;import org.springframework.transaction.annotation.Transactional;
import java.util.*;import static org.assertj.core.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;
class AuditSubmissionTransactionTest {
 WmNewsMapper news;AuditTaskMapper tasks;AuditTaskCoordinator audits;WemediaService service;
 @BeforeEach void setup(){
  UserContext.setUserId(8L);UserContext.setUserType("WEMEDIA");news=mock(WmNewsMapper.class);tasks=mock(AuditTaskMapper.class);audits=mock(AuditTaskCoordinator.class);
  service=new WemediaService(mock(WmUserMapper.class),news,mock(WmMaterialMapper.class),mock(WmNewsMaterialMapper.class),tasks,audits,new ObjectMapper(),new BCryptPasswordEncoder(),mock(JwtTokenService.class));
 }
 @AfterEach void clear(){UserContext.clear();}
 private WmNews draft(){WmNews n=new WmNews();n.setId(22L);n.setUserId(8L);n.setStatus(WmNewsStatus.DRAFT);n.setAuditVersion(0L);n.setContent("[]");n.setCoverImages("[]");return n;}
 @Test void submitMethodHasLocalTransactionAndCreatesPendingTask(){
  assertThat(WemediaService.class.getDeclaredMethods()).filteredOn(m->m.getName().equals("submit")).singleElement().satisfies(m->assertThat(m.isAnnotationPresent(Transactional.class)).isTrue());
  WmNews before=draft(),after=draft();after.setStatus(WmNewsStatus.AUDITING);after.setAuditVersion(1L);when(news.selectById(22L)).thenReturn(before,after);when(news.update(isNull(),any())).thenReturn(1);when(audits.trail(22L)).thenReturn(List.of());
  service.submit(22L);
  verify(tasks).insert(argThat((AuditTask t)->"PENDING".equals(t.getStatus())&&t.getAttemptNo()==0&&"PENDING".equals(t.getDispatchStatus())&&t.getAuditVersion()==1));
 }
 @Test void taskInsertFailureIsNotSwallowedSoArticleTransactionRollsBack(){
  WmNews before=draft(),after=draft();after.setStatus(WmNewsStatus.AUDITING);after.setAuditVersion(1L);when(news.selectById(22L)).thenReturn(before,after);when(news.update(isNull(),any())).thenReturn(1);when(tasks.insert(any(AuditTask.class))).thenThrow(new IllegalStateException("insert failed"));
  assertThatThrownBy(()->service.submit(22L)).isInstanceOf(IllegalStateException.class).hasMessageContaining("insert failed");
 }
}
