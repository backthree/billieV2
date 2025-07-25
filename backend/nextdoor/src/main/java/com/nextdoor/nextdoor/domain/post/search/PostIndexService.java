package com.nextdoor.nextdoor.domain.post.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.nextdoor.nextdoor.domain.post.domain.Post;
import com.nextdoor.nextdoor.domain.post.exception.PostIndexException;
import com.nextdoor.nextdoor.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostIndexService {

  private static final int BATCH_SIZE = 1000;
  private final PostRepository postRepository;
  private final ElasticsearchClient esClient;
  private final IndexLockService indexLockService;
  private final PostSearchRepository elasticSearchRepository;
  private final ExecutorService executor = Executors.newFixedThreadPool(2);

  @Scheduled(cron = "0 0 3 * * *")
  @Transactional(readOnly = true)
  public void reindexAll() {
    if (!indexLockService.acquireFullIndexLock()) {
      throw new PostIndexException("이미 전체 인덱싱 중입니다.");
    }
    try {
      esClient.deleteByQuery(d -> d
              .index("posts")
              .query(q -> q.matchAll(m -> m))
      );

      long lastId = 0L;
      List<Future<?>> futures = new ArrayList<>();

      while (true) {
        var batch = findBatchPost(lastId);
        if (batch.isEmpty()) break;

        List<Post> batchCopy = new ArrayList<>(batch);
        futures.add(
                executor.submit(() -> indexBatch(batchCopy))
        );
        lastId = batch.get(batch.size() - 1).getId();
      }
      for (Future<?> f : futures) {
        f.get();
      }
    } catch (Exception e) {
      throw new PostIndexException("전체 인덱싱 중 오류", e);
    } finally {
      indexLockService.releaseFullIndexLock();
    }
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public List<Post> findBatchPost(long lastId) {
    return postRepository.findPostsAfter(
            lastId,
            PageRequest.of(0, BATCH_SIZE, Sort.by("id"))
    );
  }

  private void indexBatch(List<Post> posts) {
    try {
      var operations = new ArrayList<BulkOperation>(posts.size());
      for (Post post : posts) {
        PostDocument doc = new PostDocument();
        doc.copyFrom(post);
        operations.add(
                BulkOperation.of(b -> b
                        .index(idx -> idx
                                .index("posts")
                                .id(post.getId().toString())
                                .document(doc)
                        )
                )
        );
      }
      BulkRequest bulkRequest = BulkRequest.of(b -> b.operations(operations));
      BulkResponse response = esClient.bulk(bulkRequest);

      if (response.errors()) {
        log.error("Bulk 색인 오류: {}",
                response.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> item.error().reason())
                        .toList()
        );
      } else {
        log.info("Indexed batch of {} documents", posts.size());
      }
    } catch (IOException e) {
      log.error("Bulk 실행 중 IOException", e);
    }
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