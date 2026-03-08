package com.esg.proposal.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Must be at least 32 chars for HMAC-SHA256
    private static final String SECRET = "test-secret-key-for-unit-tests-1234";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtil.generateToken("EMP001", "USER");

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    void getEmployeeId_returnsCorrectSubject() {
        String token = jwtUtil.generateToken("EMP001", "USER");

        assertThat(jwtUtil.getEmployeeId(token)).isEqualTo("EMP001");
    }

    @Test
    void getRole_returnsCorrectRole() {
        String token = jwtUtil.generateToken("EMP001", "ADMIN");

        assertThat(jwtUtil.getRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void isValid_withValidToken_returnsTrue() {
        String token = jwtUtil.generateToken("EMP001", "USER");

        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    void isValid_withTamperedToken_returnsFalse() {
        assertThat(jwtUtil.isValid("this.is.not.a.valid.token")).isFalse();
    }

    @Test
    void isValid_withExpiredToken_returnsFalse() {
        JwtUtil expiredJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredJwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(expiredJwtUtil, "expirationMs", -1000L); // already expired

        String token = expiredJwtUtil.generateToken("EMP001", "USER");

        assertThat(jwtUtil.isValid(token)).isFalse();
    }

    @Test
    void parseToken_withValidToken_returnsCorrectClaims() {
        String token = jwtUtil.generateToken("EMP002", "USER");

        var claims = jwtUtil.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("EMP002");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }
}
