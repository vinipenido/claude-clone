package com.vinicius.claude_clone.exception;

public class ConversationNotFoundException extends RuntimeException{

    public ConversationNotFoundException(Long id) {
        super("Conversa não encontrada: " + id);
    }
}
