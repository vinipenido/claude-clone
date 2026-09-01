package com.vinicius.claude_clone.dto;

import com.vinicius.claude_clone.model.Message;
import java.time.LocalDateTime;

public record MessageDto(Long id, Message.Role role, String content, LocalDateTime createdAt) {
}
