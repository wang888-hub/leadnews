package com.aaliyun.leadnews.ai;import com.aaliyun.leadnews.ai.support.SecretRedactor;import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.assertThat;
class SecretRedactionTest {@Test void removesApiKeyShape(){String raw="provider rejected "+"sk-"+"abcdefghijklmnopqrstuvwxyz012345";assertThat(new SecretRedactor().redact(raw)).isEqualTo("provider rejected [REDACTED]");}}
