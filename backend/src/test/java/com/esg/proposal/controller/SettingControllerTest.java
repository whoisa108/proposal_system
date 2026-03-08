package com.esg.proposal.controller;

import com.esg.proposal.service.SettingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingControllerTest {

    @Mock
    private SettingService settingService;

    @InjectMocks
    private SettingController settingController;

    @Test
    void getDeadline_whenDeadlineSet_returnsDeadlineAndIsPassed() {
        Instant deadline = Instant.parse("2030-01-01T00:00:00Z");
        when(settingService.getDeadline()).thenReturn(Optional.of(deadline));
        when(settingService.isDeadlinePassed()).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = settingController.getDeadline();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("deadline", deadline.toString());
        assertThat(response.getBody()).containsEntry("isPassed", false);
    }

    @Test
    void getDeadline_whenNoDeadline_returnsNullDeadline() {
        when(settingService.getDeadline()).thenReturn(Optional.empty());
        when(settingService.isDeadlinePassed()).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = settingController.getDeadline();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("deadline")).isNull();
        assertThat(response.getBody()).containsEntry("isPassed", false);
    }

    @Test
    void getDeadline_whenDeadlinePassed_returnsIsPassedTrue() {
        Instant deadline = Instant.parse("2000-01-01T00:00:00Z");
        when(settingService.getDeadline()).thenReturn(Optional.of(deadline));
        when(settingService.isDeadlinePassed()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = settingController.getDeadline();

        assertThat(response.getBody()).containsEntry("isPassed", true);
    }
}
