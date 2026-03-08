package com.esg.proposal.audit;

import com.esg.proposal.model.AuditLog;
import com.esg.proposal.model.User;
import com.esg.proposal.repository.AuditLogRepository;
import com.esg.proposal.repository.UserRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("EMP001", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @SuppressWarnings("unchecked")
    private void setupJoinPoint(String action, Object... args) throws Exception {
        Method method = SampleController.class.getMethod("sampleMethod", String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
    }

    // Helper controller class to provide a real annotated method for reflection
    static class SampleController {
        @Audited(action = "CREATE_PROPOSAL")
        public void sampleMethod(String id) {}
    }

    @Test
    void logAudit_withKnownUser_savesAuditLogWithUserName() throws Exception {
        setupJoinPoint("CREATE_PROPOSAL", "prop-id-1");
        User user = new User();
        user.setName("王大陸");
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(user));

        auditAspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getOperatorId()).isEqualTo("EMP001");
        assertThat(log.getOperatorName()).isEqualTo("王大陸");
        assertThat(log.getAction()).isEqualTo("CREATE_PROPOSAL");
        assertThat(log.getTargetId()).isEqualTo("prop-id-1");
    }

    @Test
    void logAudit_withUnknownUser_usesEmployeeIdAsName() throws Exception {
        setupJoinPoint("CREATE_PROPOSAL", "prop-id-1");
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.empty());

        auditAspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getOperatorName()).isEqualTo("EMP001");
    }

    @Test
    void logAudit_withNoArgs_setsEmptyTargetId() throws Exception {
        setupJoinPoint("CREATE_PROPOSAL");
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.empty());
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        auditAspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetId()).isEmpty();
    }

    @Test
    void logAudit_withRequestContext_capturesIp() throws Exception {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        ServletRequestAttributes attrs = new ServletRequestAttributes(httpRequest);
        RequestContextHolder.setRequestAttributes(attrs);

        setupJoinPoint("CREATE_PROPOSAL", "prop-id-1");
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.empty());

        auditAspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIp()).isEqualTo("192.168.1.1");
    }

    @Test
    void logAudit_withRemoteAddr_capturesRemoteIp() throws Exception {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("10.0.0.5");
        ServletRequestAttributes attrs = new ServletRequestAttributes(httpRequest);
        RequestContextHolder.setRequestAttributes(attrs);

        setupJoinPoint("CREATE_PROPOSAL", "prop-id-1");
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.empty());

        auditAspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIp()).isEqualTo("10.0.0.5");
    }

    @Test
    void logAudit_noRequestContext_setsIpAsUnknown() throws Exception {
        // No RequestContextHolder set
        setupJoinPoint("CREATE_PROPOSAL", "prop-id-1");
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.empty());

        auditAspect.logAudit(joinPoint);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIp()).isEqualTo("unknown");
    }

    @Test
    void logAudit_repositoryThrows_doesNotPropagateException() throws Exception {
        setupJoinPoint("CREATE_PROPOSAL", "prop-id-1");
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("DB error")).when(auditLogRepository).save(any());

        // Should not throw - audit failure is silent
        assertThatCode(() -> auditAspect.logAudit(joinPoint)).doesNotThrowAnyException();
    }
}
