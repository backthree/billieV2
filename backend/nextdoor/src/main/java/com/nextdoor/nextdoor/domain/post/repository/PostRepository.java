package com.nextdoor.nextdoor.domain.post.repository;

import com.nextdoor.nextdoor.domain.post.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.id > :lastId ORDER BY p.id ASC")
    List<Post> findPostsAfter(
            @Param("lastId") Long lastId,
            Pageable pageable
    );
}