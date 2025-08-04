package com.nextdoor.nextdoor.domain.chat.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB 컬렉션으로 전환된 UnreadCounter
 */
@Document(collection = "unread_counters")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadCounter {
    @Id
    private String id;          // 예: roomId + "_" + userId

    private Long roomId;
    private Long userId;
    private long unreadCount;
}
