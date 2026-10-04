package com.squadup.aop;

import com.squadup.dto.AuthDtos.LoginRequest;
import com.squadup.dto.AuthDtos.LoginResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Security audit trail written to its own log file (logs/audit.log, see logback-spring.xml).
 * Login attempts and account changes are recorded here, and the services contain no audit code.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    @AfterReturning(pointcut = "execution(* com.squadup.service.AuthService.login(..))", returning = "response")
    public void loginSucceeded(LoginResponse response) {
        audit.info("LOGIN_SUCCESS user={} id={}", response.user().username(), response.user().id());
    }

    @AfterThrowing(pointcut = "execution(* com.squadup.service.AuthService.login(..)) && args(request)", throwing = "ex")
    public void loginFailed(LoginRequest request, Exception ex) {
        audit.warn("LOGIN_FAILED login={} reason=\"{}\"", request.username(), ex.getMessage());
    }

    /** {@code @After} runs whether the method returned or threw, like a finally block. */
    @After("execution(* com.squadup.service.UserService.deleteAccount(..)) && args(userId)")
    public void accountDeletion(JoinPoint jp, Long userId) {
        audit.info("ACCOUNT_DELETE_ATTEMPT id={}", userId);
    }

    @AfterReturning("execution(* com.squadup.service.UserService.changePassword(..)) && args(userId, ..)")
    public void passwordChanged(Long userId) {
        audit.info("PASSWORD_CHANGED id={}", userId);
    }
}
