package com.esg.proposal.controller;

import com.esg.proposal.dto.LoginRequest;
import com.esg.proposal.dto.RegisterRequest;
import com.esg.proposal.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

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
    }

    @Test
    void register_success_returns200WithMessage() {
        doNothing().when(authService).register(registerRequest);

        ResponseEntity<Map<String, String>> response = authController.register(registerRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "註冊成功");
        verify(authService).register(registerRequest);
    }

    @Test
    void register_serviceThrows_propagatesException() {
        doThrow(new RuntimeException("工號已被註冊")).when(authService).register(registerRequest);

        assertThatThrownBy(() -> authController.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("工號已被註冊");
    }

    @Test
    void login_success_returns200WithToken() {
        Map<String, Object> loginResult = Map.of(
                "token", "jwt-token",
                "employeeId", "EMP001",
                "name", "Test User",
                "department", "AAID",
                "role", "USER"
        );
        when(authService.login(loginRequest)).thenReturn(loginResult);

        ResponseEntity<Map<String, Object>> response = authController.login(loginRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("token", "jwt-token");
        assertThat(response.getBody()).containsEntry("employeeId", "EMP001");
    }

    @Test
    void login_serviceThrows_propagatesException() {
        when(authService.login(loginRequest)).thenThrow(new RuntimeException("工號或密碼錯誤"));

        assertThatThrownBy(() -> authController.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("工號或密碼錯誤");
    }
}
