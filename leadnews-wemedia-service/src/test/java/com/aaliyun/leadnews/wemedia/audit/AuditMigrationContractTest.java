package com.aaliyun.leadnews.wemedia.audit;
import org.junit.jupiter.api.Test;import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
class AuditMigrationContractTest {
 @Test void eventAndNewsVersionAreUniqueAndExecutionColumnsExist()throws Exception{
  String v4=new String(getClass().getResourceAsStream("/db/migration/V4__add_async_article_audit.sql").readAllBytes(),StandardCharsets.UTF_8);
  String v5=new String(getClass().getResourceAsStream("/db/migration/V5__harden_audit_task_execution.sql").readAllBytes(),StandardCharsets.UTF_8);
  String v6=new String(getClass().getResourceAsStream("/db/migration/V6__sensitive_word_version_and_audit_trace.sql").readAllBytes(),StandardCharsets.UTF_8);
  assertThat(v4).contains("UNIQUE KEY uk_audit_task_event(event_id)","UNIQUE KEY uk_audit_task_news_version(news_id,audit_version)");
  assertThat(v5).contains("attempt_no","dispatch_status","dispatch_retry_count","next_dispatch_time","last_dispatched_at","started_at","finished_at");
  assertThat(v6).contains("wm_sensitive_word_version","sensitive_word_version","UNIQUE KEY uk_sensitive_word(word)");
 }
}
