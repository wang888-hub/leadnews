package com.aaliyun.leadnews.wemedia.audit;
import com.aaliyun.leadnews.model.ai.*;
import org.springframework.stereotype.Component;
@Component
public class AiAuditResultValidator {
 public void validate(ArticleAuditResult r){
  if(r==null||r.decision()==null||r.riskLevel()==null||!Double.isFinite(r.confidence())||r.confidence()<0||r.confidence()>1)
   throw new AiAuditExceptions.InvalidResponse("Required field or confidence is invalid");
  String reason=r.reason();
  if(reason==null||reason.isBlank()||reason.length()>500)throw new AiAuditExceptions.InvalidResponse("Reason is invalid");
  if(r.riskTags()==null||r.riskTags().size()>20||r.riskTags().stream().anyMatch(x->x==null||x.isBlank()||x.length()>64))
   throw new AiAuditExceptions.InvalidResponse("Risk tags are invalid");
  if(r.decision()==ModerationDecision.PASS&&r.riskLevel()==RiskLevel.HIGH)
   throw new AiAuditExceptions.InvalidResponse("PASS cannot carry HIGH risk");
  if(r.decision()==ModerationDecision.REJECT&&r.riskLevel()==RiskLevel.LOW)
   throw new AiAuditExceptions.InvalidResponse("REJECT cannot carry LOW risk");
 }
}
