package com.aaliyun.leadnews.ai.support;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;
@Component public class SecretRedactor {
 private static final Pattern KEY=Pattern.compile("sk-[A-Za-z0-9_-]{8,}");
 public String redact(String value){return value==null?null:KEY.matcher(value).replaceAll("[REDACTED]");}
}
