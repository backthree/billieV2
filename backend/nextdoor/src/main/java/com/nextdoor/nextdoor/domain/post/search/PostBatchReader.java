package com.nextdoor.nextdoor.domain.post.search;

import com.nextdoor.nextdoor.domain.post.domain.Post;
import com.nextdoor.nextdoor.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostBatchReader {

    private final PostRepository postRepository;
    private static final int BATCH_SIZE = 500;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<Post> findBatchPost(long lastId) {
        return postRepository.findPostsAfter(
                lastId,
                PageRequest.of(0, BATCH_SIZE, Sort.by("id"))
        );
    }
}