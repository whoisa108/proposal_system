package com.esg.proposal.service;

import com.esg.proposal.dto.LoginRequest;
import com.esg.proposal.dto.RegisterRequest;
import com.esg.proposal.model.User;
import com.esg.proposal.repository.UserRepository;
import com.esg.proposal.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmployeeId("EMP001");
        registerRequest.setName("Test User");
        registerRequest.setDepartment("AAID");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmployeeId("EMP001");
        loginRequest.setPassword("password123");

        user = new User();
        user.setId("user-id-1");
        user.setEmployeeId("EMP001");
        user.setName("Test User");
        user.setDepartment("AAID");
        user.setPassword("encoded-password");
        user.setRole("USER");
    }

    // --- register ---

    @Test
    void register_success() {
        when(userRepository.existsByEmployeeId("EMP001")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        authService.register(registerRequest);

        verify(userRepository).save(argThat(u ->
                u.getEmployeeId().equals("EMP001") &&
                u.getName().equals("Test User") &&
                u.getDepartment().equals("AAID") &&
                u.getPassword().equals("encoded-password") &&
                u.getRole().equals("USER")
        ));
    }

    @Test
    void register_duplicateEmployeeId_throwsException() {
        when(userRepository.existsByEmployeeId("EMP001")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("工號已被註冊");

        verify(userRepository, never()).save(any());
    }

    // --- login ---

    @Test
    void login_success() {
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("EMP001", "USER")).thenReturn("jwt-token");

        Map<String, Object> result = authService.login(loginRequest);

        assertThat(result.get("token")).isEqualTo("jwt-token");
        assertThat(result.get("employeeId")).isEqualTo("EMP001");
        assertThat(result.get("name")).isEqualTo("Test User");
        assertThat(result.get("department")).isEqualTo("AAID");
        assertThat(result.get("role")).isEqualTo("USER");
    }

    @Test
    void login_userNotFound_throwsException() {
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("工號或密碼錯誤");
    }

    @Test
    void login_wrongPassword_throwsException() {
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("工號或密碼錯誤");
    }
}
