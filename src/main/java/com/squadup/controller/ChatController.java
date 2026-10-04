package com.squadup.controller;

import com.squadup.dto.ApiResponses.MessageList;
import com.squadup.security.AuthUser;
import com.squadup.service.ChatService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Chat history over REST. New messages are sent and received over STOMP (see ChatSocketController). */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** {@code GET /api/v1/chat/history/squad_3?limit=50}: newest messages, oldest first. */
    @GetMapping("/history/{roomId}")
    public MessageList history(@PathVariable String roomId,
                               @RequestParam(defaultValue = "50") @Min(1) @Max(ChatService.MAX_HISTORY) int limit,
                               @AuthenticationPrincipal AuthUser me) {
        return new MessageList(chatService.history(me.id(), roomId, limit));
    }
}
