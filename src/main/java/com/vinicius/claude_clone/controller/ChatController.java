package com.vinicius.claude_clone.controller;

import com.vinicius.claude_clone.dto.ChatRequest;
import com.vinicius.claude_clone.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.vinicius.claude_clone.dto.ConversationSummaryDto;
import com.vinicius.claude_clone.dto.MessageDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@RestController
@RequestMapping("api/chat")


public class ChatController {
    @Autowired
    private ChatService chatService;

    @PostMapping
    public SseEmitter chat(@RequestBody @Valid ChatRequest request) {
        // Timeout generoso (5 min) pra não cortar respostas longas
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        chatService.processChat(request.getConversationId(), request.getMessage(), emitter);

        return emitter;
    }

    @GetMapping
    public List<ConversationSummaryDto> listarConversas() {
        return chatService.listarConversas();
    }

    @GetMapping("/{conversationId}/messages")
    public List<MessageDto> buscarHistorico(@PathVariable Long conversationId) {
        return chatService.buscarHistorico(conversationId);
    }
}
