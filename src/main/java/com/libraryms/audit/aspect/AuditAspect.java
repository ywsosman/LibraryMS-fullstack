package com.libraryms.audit.aspect;

import com.libraryms.audit.service.AuditService;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.libraryms.common.security.CurrentUserPrincipal;

@Aspect
@Component
@Order(100)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        Object result = pjp.proceed();

        try {
            Long entityId = extractEntityId(result, pjp.getArgs());
            Long userId = null;
            String username = "system";

            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof CurrentUserPrincipal principal) {
                    userId = principal.id();
                    username = principal.username();
                } else if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                    username = auth.getName();
                }
            } catch (Exception ex) {
                log.warn("Could not resolve current actor for audit log, using system: {}", ex.getMessage());
            }

            String details = "Operation " + audited.operation() + " on " + audited.entityType() +
                    (entityId != null ? " (id: " + entityId + ")" : "");

            auditService.logEvent(userId, username, audited.entityType(), entityId, audited.operation(), details);
        } catch (Exception ex) {
            log.error("Failed to process audit in aspect: {}", ex.getMessage(), ex);
        }

        return result;
    }

    private Long extractEntityId(Object result, Object[] args) {
        if (result != null) {
            Long id = tryInvokeIdMethod(result);
            if (id != null) {
                return id;
            }
        }

        if (args != null && args.length > 0) {
            if (args[0] instanceof Long id) {
                return id;
            }
            Long id = tryInvokeIdMethod(args[0]);
            if (id != null) {
                return id;
            }
        }

        return null;
    }

    private Long tryInvokeIdMethod(Object target) {
        if (target == null) {
            return null;
        }
        try {
            Method m = target.getClass().getMethod("id");
            Object val = m.invoke(target);
            if (val instanceof Long l) {
                return l;
            }
        } catch (Exception ignored) {
        }
        try {
            Method m = target.getClass().getMethod("getId");
            Object val = m.invoke(target);
            if (val instanceof Long l) {
                return l;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
