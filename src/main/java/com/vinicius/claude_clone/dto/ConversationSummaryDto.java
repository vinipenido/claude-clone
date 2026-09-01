package com.vinicius.claude_clone.dto;

import java.time.LocalDateTime;

public record ConversationSummaryDto(Long id, String title, LocalDateTime createdAt) {
}