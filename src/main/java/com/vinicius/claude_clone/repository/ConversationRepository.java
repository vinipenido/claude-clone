package com.vinicius.claude_clone.repository;
import com.vinicius.claude_clone.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ConversationRepository extends JpaRepository<Conversation, Long> {}
