package com.vinicius.claude_clone.exception;

public class AnthropicApiException extends RuntimeException {

    public AnthropicApiException(String message, Throwable cause) {

        super(message, cause);
    }
}
