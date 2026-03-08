package com.esg.proposal.service;

import com.esg.proposal.model.Setting;
import com.esg.proposal.repository.SettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

    @Mock
    private SettingRepository settingRepository;

    @InjectMocks
    private SettingService settingService;

    private static final String DEADLINE_KEY = "DEADLINE";

    // --- getDeadline ---

    @Test
    void getDeadline_whenSet_returnsInstant() {
        Setting setting = new Setting();
        setting.setKey(DEADLINE_KEY);
        setting.setValue("2030-01-01T00:00:00Z");
        when(settingRepository.findByKey(DEADLINE_KEY)).thenReturn(Optional.of(setting));

        Optional<Instant> result = settingService.getDeadline();

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(Instant.parse("2030-01-01T00:00:00Z"));
    }

    @Test
    void getDeadline_whenNotSet_returnsEmpty() {
        when(settingRepository.findByKey(DEADLINE_KEY)).thenReturn(Optional.empty());

        Optional<Instant> result = settingService.getDeadline();

        assertThat(result).isEmpty();
    }

    // --- setDeadline ---

    @Test
    void setDeadline_createsNewSettingWhenNotExists() {
        when(settingRepository.findByKey(DEADLINE_KEY)).thenReturn(Optional.empty());

        settingService.setDeadline("2030-06-01T12:00:00Z");

        ArgumentCaptor<Setting> captor = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository).save(captor.capture());
        assertThat(captor.getValue().getKey()).isEqualTo(DEADLINE_KEY);
        assertThat(captor.getValue().getValue()).isEqualTo("2030-06-01T12:00:00Z");
    }

    @Test
    void setDeadline_updatesExistingSetting() {
        Setting existing = new Setting();
        existing.setKey(DEADLINE_KEY);
        existing.setValue("2025-01-01T00:00:00Z");
        when(settingRepository.findByKey(DEADLINE_KEY)).thenReturn(Optional.of(existing));

        settingService.setDeadline("2030-06-01T12:00:00Z");

        ArgumentCaptor<Setting> captor = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository).save(captor.capture());
        assertThat(captor.getValue().getValue()).isEqualTo("2030-06-01T12:00:00Z");
    }

    // --- isDeadlinePassed ---

    @Test
    void isDeadlinePassed_whenDeadlineInFuture_returnsFalse() {
        Setting setting = new Setting();
        setting.setKey(DEADLINE_KEY);
        setting.setValue("2099-01-01T00:00:00Z");
        when(settingRepository.findByKey(DEADLINE_KEY)).thenReturn(Optional.of(setting));

        assertThat(settingService.isDeadlinePassed()).isFalse();
    }

    @Test
    void isDeadlinePassed_whenDeadlineInPast_returnsTrue() {
        Setting setting = new Setting();
        setting.setKey(DEADLINE_KEY);
        setting.setValue("2000-01-01T00:00:00Z");
        when(settingRepository.findByKey(DEADLINE_KEY)).thenReturn(Optional.of(setting));

        assertThat(settingService.isDeadlinePassed()).isTrue();
    }

    @Test
    void isDeadlinePassed_whenNoDeadlineSet_returnsFalse() {
        when(settingRepository.findByKey(DEADLINE_KEY)).thenReturn(Optional.empty());

        assertThat(settingService.isDeadlinePassed()).isFalse();
    }
}
