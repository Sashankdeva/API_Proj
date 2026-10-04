package com.squadup.aop;

import com.squadup.dto.AuthDtos.LoginRequest;
import com.squadup.dto.AuthDtos.RegisterRequest;
import com.squadup.exception.ApiException;
import com.squadup.service.AuthService;
import com.squadup.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit II: verifies that the advices in LoggingAspect, AuditAspect and PerformanceAspect
 * really run around the service methods.
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AopAdviceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Test
    void servicesAreWrappedInAopProxies() {
        assertThat(AopUtils.isAopProxy(authService)).isTrue();
        assertThat(AopUtils.isAopProxy(userService)).isTrue();
    }

    @Test
    void beforeAndAfterReturningAdviceLogServiceCalls(CapturedOutput output) {
        authService.register(new RegisterRequest("aop_player", "aop_player@test.com", "secret123"));

        assertThat(output)
                .contains("-> AuthService.register(RegisterRequest[username=aop_player")
                .contains("<- AuthService.register returned");
        // The password must never reach the logs.
        assertThat(output).doesNotContain("secret123");
    }

    @Test
    void afterThrowingAdviceLogsRejectedCallsAndAuditsFailedLogins(CapturedOutput output) {
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody_here", "whatever")))
                .isInstanceOf(ApiException.class);

        assertThat(output)
                .contains("x  AuthService.login rejected: Account not found")
                .contains("LOGIN_FAILED login=nobody_here");
    }

    @Test
    void afterReturningAdviceAuditsSuccessfulLogins(CapturedOutput output) {
        authService.register(new RegisterRequest("audit_me", "audit_me@test.com", "secret123"));
        authService.login(new LoginRequest("audit_me", "secret123"));

        assertThat(output).contains("LOGIN_SUCCESS user=audit_me");
    }
}
