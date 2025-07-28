package com.nextdoor.nextdoor.domain.post.search;

import com.nextdoor.nextdoor.domain.post.search.dto.PostBatchResult;
import com.nextdoor.nextdoor.domain.post.search.dto.PostWithLikeCountDto;
import com.nextdoor.nextdoor.domain.post.repository.PostRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostBatchReader {

    private static final int BATCH_SIZE = 500;
    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public PostBatchResult findNextBatch(long lastId) {
        Pageable pageRequest = PageRequest.of(0, BATCH_SIZE);
        List<Long> postIds = postRepository.findPostIdsAfter(lastId, pageRequest);

        if (postIds.isEmpty()) {
            return new PostBatchResult(Collections.emptyList(), lastId);
        }

        List<PostWithLikeCountDto> postDtos = postRepository.findPostsWithLikeCountByIds(postIds);

        long nextLastId = postIds.get(postIds.size() - 1);

        return new PostBatchResult(postDtos, nextLastId);
    }
}