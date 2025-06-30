package com.nextdoor.nextdoor.domain.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLikeCount {

    @Id
    private Long postId;

    @Column(name = "like_count")
    private Long likeCount;

    @Builder
    public PostLikeCount(Long postId, Long likeCount) {
        this.postId = postId;
        this.likeCount = likeCount;
    }
}
