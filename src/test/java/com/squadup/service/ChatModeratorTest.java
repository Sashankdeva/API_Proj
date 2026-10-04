package com.squadup.service;

import com.squadup.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit test: no Spring context needed, the moderator is configured through its setters. */
class ChatModeratorTest {

    private ChatModerator moderator;

    @BeforeEach
    void setUp() {
        moderator = new ChatModerator();
        moderator.setBannedWords(List.of("noob", "trash"));
        moderator.setMaxLength(20);
    }

    @Test
    void masksBannedWordsCaseInsensitively() {
        assertThat(moderator.moderate("  you NOOB, trash talk  ")).isEqualTo("you ****, ***** talk");
    }

    @Test
    void leavesWordsThatOnlyContainABannedWord() {
        assertThat(moderator.moderate("trashcan")).isEqualTo("trashcan");
    }

    @Test
    void rejectsEmptyAndTooLongMessages() {
        assertThatThrownBy(() -> moderator.moderate("   ")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> moderator.moderate("x".repeat(21)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("max 20");
    }
}
