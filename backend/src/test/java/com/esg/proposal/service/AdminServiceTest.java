package com.esg.proposal.service;

import com.esg.proposal.model.AuditLog;
import com.esg.proposal.model.User;
import com.esg.proposal.repository.AuditLogRepository;
import com.esg.proposal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminService adminService;

    // --- getAllUsers ---

    @Test
    void getAllUsers_returnsAllUsers() {
        User u1 = new User();
        u1.setEmployeeId("EMP001");
        User u2 = new User();
        u2.setEmployeeId("EMP002");
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<User> result = adminService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmployeeId()).isEqualTo("EMP001");
    }

    // --- deleteUser ---

    @Test
    void deleteUser_success() {
        User user = new User();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        adminService.deleteUser("user-1");

        verify(userRepository).deleteById("user-1");
    }

    @Test
    void deleteUser_userNotFound_throwsException() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteUser("unknown"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("找不到使用者");

        verify(userRepository, never()).deleteById(any());
    }

    // --- getAuditLogs ---

    @Test
    void getAuditLogs_returnsLogsInDescendingOrder() {
        AuditLog log1 = new AuditLog();
        AuditLog log2 = new AuditLog();
        when(auditLogRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of(log1, log2));

        List<AuditLog> result = adminService.getAuditLogs();

        assertThat(result).hasSize(2);
        verify(auditLogRepository).findAllByOrderByTimestampDesc();
    }
}
