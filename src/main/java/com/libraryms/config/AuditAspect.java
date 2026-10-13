package com.libraryms.config;

import com.libraryms.service.AuditService;

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
        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable ex) {
            try {
                Long entityId = extractEntityId(null, pjp.getArgs());
                ActorInfo actor = resolveActor();
                String failureDetails = "FAILED: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
                auditService.logAutonomousEvent(actor.userId(), actor.username(), audited.entityType(), entityId, audited.operation(), failureDetails);
            } catch (Exception auditEx) {
                log.error("Failed to record failure audit log in aspect: {}", auditEx.getMessage(), auditEx);
            }
            throw ex;
        }

        try {
            Long entityId = extractEntityId(result, pjp.getArgs());
            ActorInfo actor = resolveActor();
            String details = "Operation " + audited.operation() + " on " + audited.entityType() +
                    (entityId != null ? " (id: " + entityId + ")" : "");

            auditService.logEvent(actor.userId(), actor.username(), audited.entityType(), entityId, audited.operation(), details);
        } catch (Exception ex) {
            log.error("Failed to process audit in aspect: {}", ex.getMessage(), ex);
        }

        return result;
    }

    private record ActorInfo(Long userId, String username) {}

    private ActorInfo resolveActor() {
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
        return new ActorInfo(userId, username);
    }

    private Long extractEntityId(Object result, Object[] args) {
        if (result != null) {
            Long id = tryInvokeIdMethod(result);
            if (id != null) {
                return id;
            }
        }

        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof Long id) {
                    return id;
                }
            }
            for (Object arg : args) {
                Long id = tryInvokeIdMethod(arg);
                if (id != null) {
                    return id;
                }
            }
        }

        return null;
    }

    private Long tryInvokeIdMethod(Object target) {
        if (target == null) {
            return null;
        }
        String[] candidateMethods = {"id", "getId", "bookId", "memberId", "userId", "copyId"};
        for (String methodName : candidateMethods) {
            try {
                Method m = target.getClass().getMethod(methodName);
                Object val = m.invoke(target);
                if (val instanceof Long l) {
                    return l;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
