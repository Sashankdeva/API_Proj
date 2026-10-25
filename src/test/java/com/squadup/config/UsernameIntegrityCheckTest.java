package com.squadup.config;

import com.squadup.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class UsernameIntegrityCheckTest {

    private final UserRepository users = mock(UserRepository.class);
    private final UsernameIntegrityCheck check = new UsernameIntegrityCheck(users);

    @Test
    void warnsAboutNamesThatDifferOnlyByCase(CapturedOutput output) {
        when(users.findUsernamesDifferingOnlyByCase()).thenReturn(List.of("admin"));

        check.run(new DefaultApplicationArguments());

        assertThat(output).contains("share a username apart from letter case").contains("[admin]");
    }

    @Test
    void staysQuietWhenEverythingIsUnique(CapturedOutput output) {
        when(users.findUsernamesDifferingOnlyByCase()).thenReturn(List.of());

        check.run(new DefaultApplicationArguments());

        assertThat(output).doesNotContain("share a username");
    }
}
