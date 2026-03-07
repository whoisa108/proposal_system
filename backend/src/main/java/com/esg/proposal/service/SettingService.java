package com.esg.proposal.service;

import com.esg.proposal.model.Setting;
import com.esg.proposal.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SettingService {

    private static final String DEADLINE_KEY = "DEADLINE";

    private final SettingRepository settingRepository;

    public Optional<Instant> getDeadline() {
        return settingRepository.findByKey(DEADLINE_KEY)
                .map(s -> Instant.parse(s.getValue()));
    }

    public void setDeadline(String isoDateTime) {
        Setting setting = settingRepository.findByKey(DEADLINE_KEY)
                .orElseGet(() -> {
                    Setting s = new Setting();
                    s.setKey(DEADLINE_KEY);
                    return s;
                });
        setting.setValue(isoDateTime);
        settingRepository.save(setting);
    }

    // 判斷目前是否已截止
    public boolean isDeadlinePassed() {
        return getDeadline()
                .map(deadline -> Instant.now().isAfter(deadline))
                .orElse(false);  // 未設定截止時間 = 未截止
    }
}
