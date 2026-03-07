package com.esg.proposal.audit;

import com.esg.proposal.model.AuditLog;
import com.esg.proposal.model.User;
import com.esg.proposal.repository.AuditLogRepository;
import com.esg.proposal.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    // 方法成功執行後才記錄
    @AfterReturning("@annotation(com.esg.proposal.audit.Audited)")
    public void logAudit(JoinPoint joinPoint) {
        try {
            // 取得當前登入者的工號
            String employeeId = (String) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();

            User user = userRepository.findByEmployeeId(employeeId).orElse(null);
            String operatorName = user != null ? user.getName() : employeeId;

            // 取得 @Audited 的 action 值
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Audited audited = method.getAnnotation(Audited.class);
            String action = audited.action();

            // 取得 request IP
            String ip = getClientIp();

            // 把方法的第一個參數當 targetId（通常是 id 或 employeeId）
            String targetId = "";
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0 && args[0] instanceof String) {
                targetId = (String) args[0];
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setOperatorId(employeeId);
            auditLog.setOperatorName(operatorName);
            auditLog.setAction(action);
            auditLog.setTargetId(targetId);
            auditLog.setDetail("args count: " + (args != null ? args.length : 0));
            auditLog.setIp(ip);

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            // Audit log 失敗不影響主要業務
            log.warn("Audit log 記錄失敗: {}", e.getMessage());
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isEmpty()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("無法取得 IP: {}", e.getMessage());
        }
        return "unknown";
    }
}
