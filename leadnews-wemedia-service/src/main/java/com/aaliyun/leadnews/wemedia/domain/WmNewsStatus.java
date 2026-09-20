package com.aaliyun.leadnews.wemedia.domain;
import java.util.Set;
public enum WmNewsStatus {
 DRAFT, SUBMITTED, AUDITING, MANUAL_REVIEW, APPROVED, REJECTED, WAITING, PUBLISHING, PUBLISHED, PUBLISH_FAILED;
 public boolean editable(){return this==DRAFT||this==REJECTED;}
 public boolean canTransitionTo(WmNewsStatus next){return switch(this){case DRAFT,REJECTED->next==SUBMITTED;case SUBMITTED->next==AUDITING;case AUDITING->Set.of(APPROVED,REJECTED,MANUAL_REVIEW).contains(next);case MANUAL_REVIEW->next==APPROVED||next==REJECTED;case APPROVED->next==WAITING||next==PUBLISHING;case WAITING->next==PUBLISHING;case PUBLISHING->next==PUBLISHED||next==PUBLISH_FAILED;case PUBLISH_FAILED->next==PUBLISHING;default->false;};}
}
