package com.vinicius.claude_clone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.vinicius.claude_clone.model.Message;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message,Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}

