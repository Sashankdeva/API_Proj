package com.squadup;

import com.squadup.controller.SquadController;
import com.squadup.matching.GameAffinityScorer;
import com.squadup.matching.LanguageScorer;
import com.squadup.matching.OpenSlotsScorer;
import com.squadup.matching.SquadMatcher;
import com.squadup.service.ChatModerator;
import com.squadup.service.ChatService;
import com.squadup.service.SquadService;
import com.squadup.service.UserService;
import com.squadup.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit I / II: the IoC container, auto-wiring, bean scopes and logging.
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AutowiringAndLoggingTest {

    private static final Logger log = LoggerFactory.getLogger(AutowiringAndLoggingTest.class);

    @Autowired
    private ApplicationContext context;

    @Autowired
    private UserService userService;          // autowired by interface type

    @Autowired
    private SquadController squadController;

    @Autowired
    private ChatService chatService;

    @Autowired
    private PasswordEncoder passwordEncoder;  // defined with @Bean in AppConfig

    @Test
    void componentScanningRegistersAnnotatedClasses() {
        assertThat(context.getBean(SquadService.class)).isNotNull();
        assertThat(context.getBean(SquadController.class)).isSameAs(squadController);
        assertThat(context.getBeanNamesForType(UserService.class)).containsExactly("userServiceImpl");
    }

    @Test
    void interfaceIsAutowiredWithItsImplementation() {
        assertThat(AopUtils.getTargetClass(userService)).isEqualTo(UserServiceImpl.class);
    }

    @Test
    void javaConfigBeansAreAvailable() {
        assertThat(passwordEncoder.matches("secret", passwordEncoder.encode("secret"))).isTrue();
        assertThat(context.getBean(ChatModerator.class).getMaxLength()).isEqualTo(2000);
    }

    @Test
    void setterInjectionProvidedTheModerator() {
        ChatService target = AopTestUtils.getTargetObject(chatService);   // unwrap the AOP proxy
        Object moderator = ReflectionTestUtils.getField(target, "moderator");
        assertThat(moderator).isSameAs(context.getBean(ChatModerator.class));
    }

    @Test
    void singletonScopeReturnsTheSameInstance() {
        assertThat(context.getBean(SquadService.class)).isSameAs(context.getBean(SquadService.class));
    }

    @Test
    void prototypeScopeReturnsANewInstanceEachTime() {
        SquadMatcher first = context.getBean(SquadMatcher.class);
        SquadMatcher second = context.getBean(SquadMatcher.class);
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void collectionAutowiringInjectsAllScorersInOrder() {
        SquadMatcher matcher = context.getBean(SquadMatcher.class);
        assertThat(matcher.getScorers())
                .extracting(s -> (Object) AopUtils.getTargetClass(s))
                .containsExactly(GameAffinityScorer.class, LanguageScorer.class, OpenSlotsScorer.class);
    }

    @Test
    void logbackWritesApplicationLogs(CapturedOutput output) {
        log.info("Logging check from {}", getClass().getSimpleName());
        assertThat(output).contains("Logging check from AutowiringAndLoggingTest");
    }
}
