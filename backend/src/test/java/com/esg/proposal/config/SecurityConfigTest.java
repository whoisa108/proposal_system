package com.esg.proposal.config;

import com.esg.proposal.security.JwtAuthFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthFilter jwtAuthFilter;

    @InjectMocks
    private SecurityConfig securityConfig;

    // ── passwordEncoder ───────────────────────────────────────────────────────

    @Test
    void passwordEncoder_returnsBCryptPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoder_encodeProducesNonNullHash() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String hash = encoder.encode("mySecret");

        assertThat(hash).isNotNull().isNotBlank();
    }

    @Test
    void passwordEncoder_encodedHashMatchesOriginalPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String raw = "password123";

        String hash = encoder.encode(raw);

        assertThat(encoder.matches(raw, hash)).isTrue();
    }

    @Test
    void passwordEncoder_wrongPasswordDoesNotMatch() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String hash = encoder.encode("correctPassword");

        assertThat(encoder.matches("wrongPassword", hash)).isFalse();
    }

    @Test
    void passwordEncoder_eachCallProducesDistinctHash() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String raw = "sameInput";

        String hash1 = encoder.encode(raw);
        String hash2 = encoder.encode(raw);

        assertThat(hash1).isNotEqualTo(hash2); // BCrypt uses random salt
    }

    // ── corsConfigurationSource ───────────────────────────────────────────────

    @Test
    void corsConfigurationSource_returnsUrlBasedCorsConfigurationSource() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
    }

    @Test
    void corsConfigurationSource_allowsViteDevOrigin() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        CorsConfiguration config = source.getCorsConfiguration(mockRequest("/api/test"));

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:5173");
    }

    @Test
    void corsConfigurationSource_allowsRequiredHttpMethods() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        CorsConfiguration config = source.getCorsConfiguration(mockRequest("/api/test"));

        assertThat(config).isNotNull();
        assertThat(config.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    void corsConfigurationSource_allowsAllHeaders() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        CorsConfiguration config = source.getCorsConfiguration(mockRequest("/api/test"));

        assertThat(config).isNotNull();
        assertThat(config.getAllowedHeaders()).containsExactly("*");
    }

    @Test
    void corsConfigurationSource_allowsCredentials() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        CorsConfiguration config = source.getCorsConfiguration(mockRequest("/api/test"));

        assertThat(config).isNotNull();
        assertThat(config.getAllowCredentials()).isTrue();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private org.springframework.mock.web.MockHttpServletRequest mockRequest(String path) {
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
