package com.esg.proposal.controller;

import com.esg.proposal.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping("/deadline")
    public ResponseEntity<Map<String, Object>> getDeadline() {
        Map<String, Object> response = new HashMap<>();
        response.put("deadline", settingService.getDeadline().map(Object::toString).orElse(null));
        response.put("isPassed", settingService.isDeadlinePassed());
        return ResponseEntity.ok(response);
    }
}
