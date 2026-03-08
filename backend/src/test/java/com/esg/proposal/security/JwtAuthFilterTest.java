package com.esg.proposal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_withValidBearerToken_setsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.isValid("valid-token")).thenReturn(true);
        when(jwtUtil.getEmployeeId("valid-token")).thenReturn("EMP001");
        when(jwtUtil.getRole("valid-token")).thenReturn("USER");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("EMP001");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withInvalidToken_doesNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtUtil.isValid("invalid-token")).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNoAuthorizationHeader_doesNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).isValid(any());
    }

    @Test
    void doFilterInternal_withNonBearerHeader_doesNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).isValid(any());
    }

    @Test
    void doFilterInternal_withAdminRole_setsAdminAuthority() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtil.isValid("admin-token")).thenReturn(true);
        when(jwtUtil.getEmployeeId("admin-token")).thenReturn("ADMIN001");
        when(jwtUtil.getRole("admin-token")).thenReturn("ADMIN");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
