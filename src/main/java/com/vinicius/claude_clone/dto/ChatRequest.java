package com.vinicius.claude_clone.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {

    private Long conversationId; // null = cria conversa nova

    @NotBlank(message = "A mensagem não pode estar vazia")
    private String message;

    public Long getConversationId() {
        return conversationId; }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId; }

    public String getMessage() {
        return message; }

    public void setMessage(String message) {
        this.message = message; }
}