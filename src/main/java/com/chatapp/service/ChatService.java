package com.chatapp.service;

import com.chatapp.dto.ChatNotification;
import com.chatapp.dto.ConversationDTO;
import com.chatapp.dto.MessageDTO;
import com.chatapp.dto.UserDTO;
import com.chatapp.model.Conversation;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Conversation getOrCreateConversation(Long user1Id, Long user2Id) {
        // Check both orderings
        Optional<Conversation> existing = conversationRepository.findByUser1IdAndUser2Id(user1Id, user2Id);
        if (existing.isPresent()) {
            return existing.get();
        }
        Optional<Conversation> reversed = conversationRepository.findByUser1IdAndUser2Id(user2Id, user1Id);
        if (reversed.isPresent()) {
            return reversed.get();
        }

        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + user1Id));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + user2Id));

        Conversation conversation = Conversation.builder()
                .user1(user1)
                .user2(user2)
                .createdAt(LocalDateTime.now())
                .status("PENDING")
                .build();

        return conversationRepository.save(conversation);
    }

    public List<ConversationDTO> getConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId);
        return conversations.stream()
                .filter(c -> "ACCEPTED".equals(c.getStatus()))
                .map(conv -> toConversationDTO(conv, userId))
                .collect(Collectors.toList());
    }

    public List<ConversationDTO> getPendingRequests(Long userId) {
        List<Conversation> pending = conversationRepository.findByUser2IdAndStatus(userId, "PENDING");
        return pending.stream()
                .map(conv -> toConversationDTO(conv, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationDTO acceptConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        if (!conversation.getUser2().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the recipient can accept this conversation");
        }
        conversation.setStatus("ACCEPTED");
        Conversation saved = conversationRepository.save(conversation);

        ChatNotification notification = ChatNotification.builder()
                .type("CONVERSATION_ACCEPTED")
                .conversationId(conversationId)
                .build();
        messagingTemplate.convertAndSend("/topic/status/" + conversation.getUser1().getId(), notification);

        return toConversationDTO(saved, userId);
    }

    @Transactional
    public ConversationDTO rejectConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        if (!conversation.getUser2().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the recipient can reject this conversation");
        }
        conversation.setStatus("REJECTED");
        Conversation saved = conversationRepository.save(conversation);
        return toConversationDTO(saved, userId);
    }

    public List<MessageDTO> getMessages(Long conversationId, Long userId) {
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageDTO sendMessage(ChatNotification notification, Long senderId) {
        Conversation conversation = conversationRepository.findById(notification.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + notification.getConversationId()));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + senderId));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(notification.getContent())
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        Message saved = messageRepository.save(message);
        return toMessageDTO(saved);
    }

    @Transactional
    public void markAsRead(Long conversationId, Long userId) {
        messageRepository.markAllAsRead(conversationId, userId);
    }

    public void sendTypingIndicator(ChatNotification notification) {
        // Just broadcast — do not persist
        messagingTemplate.convertAndSend(
                "/topic/typing/" + notification.getConversationId(), notification);
    }

    private ConversationDTO toConversationDTO(Conversation conv, Long requestingUserId) {
        User otherUser = conv.getUser1().getId().equals(requestingUserId)
                ? conv.getUser2()
                : conv.getUser1();

        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conv.getId());
        String lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1).getContent();
        long unreadCount = messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(conv.getId(), requestingUserId);

        return ConversationDTO.builder()
                .id(conv.getId())
                .otherUser(UserDTO.builder()
                        .id(otherUser.getId())
                        .name(otherUser.getName())
                        .email(otherUser.getEmail())
                        .status(otherUser.getStatus())
                        .build())
                .lastMessage(lastMessage)
                .unreadCount((int) unreadCount)
                .createdAt(conv.getCreatedAt())
                .status(conv.getStatus())
                .build();
    }

    private MessageDTO toMessageDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .read(message.isRead())
                .build();
    }
}
