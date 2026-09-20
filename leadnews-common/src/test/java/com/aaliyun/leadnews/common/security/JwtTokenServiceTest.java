package com.aaliyun.leadnews.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;

class JwtTokenServiceTest {
    private final JwtTokenService service = new JwtTokenService("stage3-test-secret-at-least-32-bytes-long");
    @Test void createsAndParsesToken() {
        var principal = service.parse(service.createToken(42L, "APP", Duration.ofMinutes(5)));
        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.clientType()).isEqualTo("APP");
    }
    @Test void rejectsExpiredToken() {
        String token = service.createToken(42L, "APP", Duration.ofSeconds(-1));
        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }
    @Test void rejectsWeakSecret() {
        assertThatThrownBy(() -> new JwtTokenService("short")).isInstanceOf(IllegalArgumentException.class);
    }
}
