package com.vinicius.claude_clone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ClaudeCloneApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClaudeCloneApplication.class, args);
	}

}
