package com.chatapp.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {

    private Long id;
    private UserDTO otherUser;
    private String status;
    private String lastMessage;
    private int unreadCount;
    private LocalDateTime createdAt;
}
