package com.esg.proposal.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleRuntimeException_returns400WithMessage() {
        RuntimeException ex = new RuntimeException("工號已被註冊");

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "工號已被註冊");
    }

    @Test
    void handleRuntimeException_nullMessage_returnsDefaultMessage() {
        RuntimeException ex = new RuntimeException((String) null);

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "發生不明錯誤");
    }

    @Test
    void handleValidationException_returns400WithFieldErrors() throws Exception {
        // Build a MethodArgumentNotValidException via its binding result
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "employeeId", "工號不得為空"));
        bindingResult.addError(new FieldError("target", "password", "密碼不得為空"));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("employeeId", "工號不得為空");
        assertThat(response.getBody()).containsEntry("password", "密碼不得為空");
    }

    @Test
    void handleException_returns500WithGenericMessage() {
        Exception ex = new Exception("Unexpected error");

        ResponseEntity<Map<String, String>> response = handler.handleException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "伺服器發生錯誤，請稍後再試");
    }
}
