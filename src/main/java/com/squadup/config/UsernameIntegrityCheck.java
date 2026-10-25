package com.squadup.config;

import com.squadup.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Usernames are unique regardless of letter case, but a database created by an older version
 * could hold both "Admin" and "admin". Logging in with such a name is ambiguous, so the problem is
 * reported at startup, with the names, instead of surfacing later as a failed login.
 */
@Component
public class UsernameIntegrityCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsernameIntegrityCheck.class);

    private final UserRepository users;

    public UsernameIntegrityCheck(UserRepository users) {
        this.users = users;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> clashes = users.findUsernamesDifferingOnlyByCase();
        if (!clashes.isEmpty()) {
            log.warn("Several accounts share a username apart from letter case: {}. "
                    + "Rename one account of each pair, otherwise logging in with that name fails.", clashes);
        }
    }
}
