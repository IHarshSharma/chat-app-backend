package com.chatapp.controller;

import com.chatapp.dto.ChatNotification;
import com.chatapp.dto.ConversationDTO;
import com.chatapp.dto.MessageDTO;
import com.chatapp.model.Conversation;
import com.chatapp.model.User;
import com.chatapp.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // -----------------------------------------------
    // REST endpoints
    // -----------------------------------------------

    @GetMapping("/api/conversations")
    public ResponseEntity<List<ConversationDTO>> getConversations(@AuthenticationPrincipal User currentUser) {
        List<ConversationDTO> conversations = chatService.getConversations(currentUser.getId());
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/api/conversations/pending")
    public ResponseEntity<List<ConversationDTO>> getPendingRequests(@AuthenticationPrincipal User currentUser) {
        List<ConversationDTO> pending = chatService.getPendingRequests(currentUser.getId());
        return ResponseEntity.ok(pending);
    }

    @PutMapping("/api/conversations/{id}/accept")
    public ResponseEntity<ConversationDTO> acceptConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        ConversationDTO dto = chatService.acceptConversation(id, currentUser.getId());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/api/conversations/{id}/reject")
    public ResponseEntity<ConversationDTO> rejectConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        ConversationDTO dto = chatService.rejectConversation(id, currentUser.getId());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/api/conversations/{userId}")
    public ResponseEntity<ConversationDTO> startConversation(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        Conversation conversation = chatService.getOrCreateConversation(currentUser.getId(), userId);
        List<ConversationDTO> dtos = chatService.getConversations(currentUser.getId());
        ConversationDTO dto = dtos.stream()
                .filter(c -> c.getId().equals(conversation.getId()))
                .findFirst()
                .orElseGet(() -> ConversationDTO.builder()
                        .id(conversation.getId())
                        .createdAt(conversation.getCreatedAt())
                        .build());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/api/messages/{conversationId}")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User currentUser) {
        List<MessageDTO> messages = chatService.getMessages(conversationId, currentUser.getId());
        return ResponseEntity.ok(messages);
    }

    // -----------------------------------------------
    // WebSocket endpoints
    // -----------------------------------------------

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatNotification notification,
                            SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            return;
        }

        User sender = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        notification.setSenderId(sender.getId());
        notification.setSenderName(sender.getName());

        MessageDTO savedMessage = chatService.sendMessage(notification, sender.getId());

        messagingTemplate.convertAndSend(
                "/topic/messages/" + notification.getConversationId(), savedMessage);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload ChatNotification notification,
                       SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            return;
        }

        User sender = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        notification.setSenderId(sender.getId());
        notification.setSenderName(sender.getName());
        notification.setType("TYPING");

        messagingTemplate.convertAndSend(
                "/topic/typing/" + notification.getConversationId(), notification);
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ChatNotification notification,
                           SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            return;
        }

        User sender = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        chatService.markAsRead(notification.getConversationId(), sender.getId());

        ChatNotification readNotification = ChatNotification.builder()
                .type("READ")
                .conversationId(notification.getConversationId())
                .senderId(sender.getId())
                .senderName(sender.getName())
                .build();

        messagingTemplate.convertAndSend(
                "/topic/messages/" + notification.getConversationId(), readNotification);
    }
}
