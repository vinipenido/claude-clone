package com.vinicius.claude_clone.service;

import com.vinicius.claude_clone.dto.ConversationSummaryDto;
import com.vinicius.claude_clone.dto.MessageDto;
import com.vinicius.claude_clone.client.AnthropicClient;
import com.vinicius.claude_clone.dto.ConversationSummaryDto;
import com.vinicius.claude_clone.dto.MessageDto;
import com.vinicius.claude_clone.model.Conversation;
import com.vinicius.claude_clone.model.Message;
import com.vinicius.claude_clone.repository.ConversationRepository;
import com.vinicius.claude_clone.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import com.vinicius.claude_clone.exception.ConversationNotFoundException;

@Service
public class ChatService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AnthropicClient anthropicClient;

    @Async
    public void processChat(Long conversationId, String userMessage, SseEmitter emitter) {

        try {
            Conversation conversation = (conversationId != null)
                    ? conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ConversationNotFoundException(conversationId))
                    : criarNovaConversa(userMessage);

            emitter.send(SseEmitter.event().name("conversation").data(conversation.getId()));

            Message userMsg = new Message();
            userMsg.setConversation(conversation);
            userMsg.setRole(Message.Role.USER);
            userMsg.setContent(userMessage);
            messageRepository.save(userMsg);

            List<Message> historico = messageRepository
                    .findByConversationIdOrderByCreatedAtAsc(conversation.getId());

            StringBuilder respostaCompleta = new StringBuilder();

            anthropicClient.streamMessage(historico, token -> {
                respostaCompleta.append(token);
                try {
                    emitter.send(SseEmitter.event().name("token").data(token));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            });

            Message assistantMsg = new Message();
            assistantMsg.setConversation(conversation);
            assistantMsg.setRole(Message.Role.ASSISTANT);
            assistantMsg.setContent(respostaCompleta.toString());
            messageRepository.save(assistantMsg);

            emitter.complete();

        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private Conversation criarNovaConversa(String primeiraMensagem) {
        Conversation conversation = new Conversation();
        String titulo = primeiraMensagem.length() > 50
                ? primeiraMensagem.substring(0, 50) + "..."
                : primeiraMensagem;
        conversation.setTitle(titulo);
        return conversationRepository.save(conversation);
    }

    public List<ConversationSummaryDto> listarConversas() {
        return conversationRepository.findAll().stream()
                .map(c -> new ConversationSummaryDto(c.getId(), c.getTitle(), c.getCreatedAt()))
                .toList();
    }

    public List<MessageDto> buscarHistorico(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(m -> new MessageDto(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
    }
}