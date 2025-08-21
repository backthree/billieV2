package com.nextdoor.nextdoor.domain.post.search.outbox.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostDeleteEvent implements PostIndexEvent {
    private final String type = "DELETE";
    private Long postId;
    private Long version;
}