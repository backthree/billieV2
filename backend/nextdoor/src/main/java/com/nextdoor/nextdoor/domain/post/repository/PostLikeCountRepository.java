package com.nextdoor.nextdoor.domain.post.repository;

import com.nextdoor.nextdoor.domain.post.domain.PostLikeCount;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeCountRepository extends JpaRepository<PostLikeCount, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostLikeCount plc SET plc.likeCount = plc.likeCount + 1 WHERE plc.postId = :postId")
    int incrementLikeCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostLikeCount plc SET plc.likeCount = plc.likeCount - 1 WHERE plc.postId = :postId")
    int decrementLikeCount(@Param("postId") Long postId);
}