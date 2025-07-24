package com.nextdoor.nextdoor.domain.post.search;

import com.nextdoor.nextdoor.domain.post.domain.Post;
import com.nextdoor.nextdoor.domain.post.exception.PostIndexException;
import com.nextdoor.nextdoor.domain.post.repository.PostRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostIndexService {
  private static final int BATCH_SIZE = 500;

  private final PostRepository postRepository;
  private final PostSearchRepository elasticSearchRepository;
  private final IndexLockService indexLockService;
  private final MeterRegistry registry;
  private final ThreadLocal<Deque<PostDocument>> docPool =
          ThreadLocal.withInitial(ArrayDeque::new);
  private final ThreadLocal<ArrayList<PostDocument>> docsList =
          ThreadLocal.withInitial(() -> new ArrayList<>(BATCH_SIZE));

  @Scheduled(cron = "0 0 3 * * *")
  public void reindexAll() {
    if (!indexLockService.acquireFullIndexLock()) {
      throw new PostIndexException("이미 전체 인덱싱 중입니다.");
    }

    try {
      elasticSearchRepository.deleteAll();

      long lastId = 0L;
      int batchNo = 0;

      while (true) {
        List<Post> batch = postRepository.findPostsAfter(
                lastId,
                PageRequest.of(0, BATCH_SIZE, Sort.by("id"))
        );
        if (batch.isEmpty()) break;

        indexBatch(batch, batchNo++);
        lastId = batch.get(batch.size() - 1).getId();
      }
    } finally {
      indexLockService.releaseFullIndexLock();
    }
  }

  public void indexBatch(List<Post> batch, int batchNo) {
    Deque<PostDocument> pool = docPool.get();
    ArrayList<PostDocument> docs = docsList.get();

    docs.clear();
    docs.ensureCapacity(batch.size());

    for (Post post : batch) {
      PostDocument doc;
      if (pool.isEmpty()) {
        doc = new PostDocument();
      } else {
        doc = pool.pop();
      }
      doc.copyFrom(post);
      docs.add(doc);
    }

    elasticSearchRepository.saveAll(docs);

    registry.counter("post.index.batch.success",
                    "batchNo", String.valueOf(batchNo))
            .increment(docs.size());
    log.info("[Index][batch:{}][size:{}] 성공", batchNo, docs.size());

    pool.addAll(docs);
  }

  @Transactional
  public void indexSinglePost(Long postId) {
    Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostIndexException("ID가 " + postId + "인 게시물이 존재하지 않습니다."));

    PostDocument doc = toDocument(post);

    if(indexLockService.isFullIndexLocked()){
      indexLockService.addToPendingIndexQueue(doc);
      throw new PostIndexException("이미 전체 인덱싱 중입니다. 배치 색인을 기다려주세요.");
    } else {
      elasticSearchRepository.save(doc);
      log.info("  단건 색인 완료: {} 건 색인", doc.getId());
    }
  }

  @Transactional
  public void deleteSingleIndex(Long postId) {
    Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostIndexException("ID가 " + postId + "인 게시물이 존재하지 않습니다."));

    if(indexLockService.isFullIndexLocked()){
      throw new PostIndexException("이미 전체 인덱싱 중입니다.");
    }

    elasticSearchRepository.deleteById(post.getId());
    log.info("  단건 삭제 완료: {} 건 색인", post.getId());
  }

  private PostDocument toDocument(Post p) {

    return PostDocument.builder()
            .id(p.getId())
            .title(p.getTitle())
            .content(p.getContent())
            .rentalFee(p.getRentalFee())
            .deposit(p.getDeposit())
            .address(p.getAddress())
            .category(p.getCategory().name())
            .authorId(p.getAuthorId())
            .likeCount(p.getLikeCount())
            .createdAt(p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
            .build();
  }
}