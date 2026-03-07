package com.esg.proposal.service;

import com.esg.proposal.model.AuditLog;
import com.esg.proposal.model.User;
import com.esg.proposal.repository.AuditLogRepository;
import com.esg.proposal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到使用者"));
        userRepository.deleteById(userId);
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}
