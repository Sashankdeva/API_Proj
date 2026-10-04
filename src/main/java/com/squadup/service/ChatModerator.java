package com.squadup.service;

import com.squadup.exception.ApiException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Cleans up chat messages before they are stored: trims them, enforces the length limit and
 * masks banned words, to keep toxicity out of squad and DM chats.
 *
 * Deliberately a plain class without {@code @Component}: it is registered in
 * {@code AppConfig#chatModerator} and configured through its setters (setter injection).
 */
public class ChatModerator {

    private Pattern bannedPattern;
    private int maxLength = 2000;

    public void setBannedWords(List<String> bannedWords) {
        List<String> words = bannedWords == null ? List.of() : bannedWords.stream()
                .map(String::trim).filter(w -> !w.isEmpty()).map(Pattern::quote).toList();
        this.bannedPattern = words.isEmpty()
                ? null
                : Pattern.compile("\\b(" + String.join("|", words) + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    /**
     * @return the text to store
     * @throws ApiException if the message is empty or too long
     */
    public String moderate(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("Message cannot be empty.");
        }
        if (trimmed.length() > maxLength) {
            throw ApiException.badRequest("Message is too long (max " + maxLength + " characters).");
        }
        if (bannedPattern == null) {
            return trimmed;
        }
        return bannedPattern.matcher(trimmed).replaceAll(m -> "*".repeat(m.group().length()));
    }
}
