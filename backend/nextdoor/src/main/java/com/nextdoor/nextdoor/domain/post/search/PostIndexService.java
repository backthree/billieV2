package com.nextdoor.nextdoor.domain.post.search;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.nextdoor.nextdoor.domain.post.domain.Post;
import com.nextdoor.nextdoor.domain.post.exception.PostIndexException;
import com.nextdoor.nextdoor.domain.post.repository.PostRepository;
import com.nextdoor.nextdoor.domain.post.search.dto.PostBatchResult;
import com.nextdoor.nextdoor.domain.post.search.dto.PostWithLikeCountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostIndexService {

  private static final int MAX_IN_FLIGHT_TASKS = 2;

  private final PostRepository postRepository;
  private final ElasticsearchClient esClient;
  private final ElasticsearchAsyncClient asyncEsClient;
  private final IndexLockService indexLockService;
  private final PostSearchRepository elasticSearchRepository;
  private final PostBatchReader postBatchReader;

  @Scheduled(cron = "0 0 3 * * *")
  public void reindexAll() {
    if (!indexLockService.acquireFullIndexLock()) {
      throw new PostIndexException("이미 전체 인덱싱 중입니다.");
    }

    try {
      esClient.deleteByQuery(d -> d.index("posts").query(q -> q.matchAll(m -> m)));

      long lastId = 0L;
      List<CompletableFuture<Void>> futures = new ArrayList<>();

      while (true) {
        PostBatchResult result = postBatchReader.findNextBatch(lastId);
        List<PostWithLikeCountDto> postDtos = result.getPosts();

        if (postDtos.isEmpty()) {
          break;
        }

        List<BulkOperation> ops = new ArrayList<>(postDtos.size());
        for (PostWithLikeCountDto dto : postDtos) {
          PostDocument doc = toDocument(dto);
          ops.add(
                  BulkOperation.of(b -> b
                          .index(idx -> idx
                                  .index("posts")
                                  .id(dto.getPostId().toString())
                                  .document(doc))));
        }

        futures.add(bulkIndexAsync(ops));
        lastId = result.getLastId();

        if (futures.size() >= MAX_IN_FLIGHT_TASKS) {
          futures.remove(0).join();
        }
      }

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      log.info("전체 인덱싱 완료");

    } catch (Exception e) {
      throw new PostIndexException("전체 인덱싱 중 오류", e);
    } finally {
      indexLockService.releaseFullIndexLock();
    }
  }

  private CompletableFuture<Void> bulkIndexAsync(List<BulkOperation> operations) {
    BulkRequest bulkRequest = BulkRequest.of(b -> b.operations(operations));
    return asyncEsClient.bulk(bulkRequest)
            .thenAccept(response -> {
              if (response.errors()) {
                log.error("Bulk 색인 오류: {}", response.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> item.error().reason()).toList());
              } else {
                log.info("Indexed {} documents", operations.size());
              }
            })
            .exceptionally(throwable -> {
              log.error("Bulk 실행 중 예외 발생", throwable);
              return null;
            });
  }

  public void indexSinglePost(Long postId) {
    PostWithLikeCountDto dto = postRepository.findDtoById(postId)
            .orElseThrow(() -> new PostIndexException("ID가 " + postId + "인 게시물이 존재하지 않습니다."));

    PostDocument doc = toDocument(dto);

    if (indexLockService.isFullIndexLocked()) {
      indexLockService.addToPendingIndexQueue(doc);
      throw new PostIndexException("이미 전체 인덱싱 중입니다. 배치 색인을 기다려주세요.");
    } else {
      elasticSearchRepository.save(doc);
      log.info("단건 색인 완료: {}", doc.getId());
    }
  }

  @Transactional
  public void deleteSingleIndex(Long postId) {
    Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostIndexException("ID가 " + postId + "인 게시물이 존재하지 않습니다."));

    if (indexLockService.isFullIndexLocked()) {
      throw new PostIndexException("이미 전체 인덱싱 중입니다.");
    }

    elasticSearchRepository.deleteById(post.getId());
    log.info("단건 삭제 완료: {}", post.getId());
  }

  private PostDocument toDocument(PostWithLikeCountDto dto) {
    PostDocument.GeoPoint location = null;
    if (dto.getLatitude() != null && dto.getLongitude() != null) {
      location = new PostDocument.GeoPoint(dto.getLatitude(), dto.getLongitude());
    }

    return PostDocument.builder()
            .id(dto.getPostId())
            .title(dto.getTitle())
            .content(dto.getContent())
            .rentalFee(dto.getRentalFee())
            .deposit(dto.getDeposit())
            .address(dto.getAddress())
            .location(location)
            .category(dto.getCategory() != null ? dto.getCategory().name() : null)
            .likeCount(dto.getLikeCount().intValue())
            .createdAt(dto.getCreatedAt())
            .build();
  }
}