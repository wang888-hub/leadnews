package com.aaliyun.leadnews.wemedia.audit;
final class AiAuditExceptions {
 private AiAuditExceptions(){}
 static final class CapacityBusy extends RuntimeException{}
 static final class CircuitOpen extends RuntimeException{}
 static final class InvalidResponse extends RuntimeException { InvalidResponse(String m){super(m);} }
}
