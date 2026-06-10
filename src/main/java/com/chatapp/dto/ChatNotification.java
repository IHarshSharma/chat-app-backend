package com.chatapp.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotification {

    private String type; // "MESSAGE", "TYPING", "READ", "STATUS"
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime timestamp;
}
