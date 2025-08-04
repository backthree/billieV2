package com.nextdoor.nextdoor.domain.post.search.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdoor.nextdoor.domain.post.exception.PostIndexException;
import com.nextdoor.nextdoor.domain.post.repository.PostRepository;
import com.nextdoor.nextdoor.domain.post.search.IndexLockService;
import com.nextdoor.nextdoor.domain.post.search.PostBatchReader;
import com.nextdoor.nextdoor.domain.post.search.PostDocument;
import com.nextdoor.nextdoor.domain.post.search.PostSearchRepository;
import com.nextdoor.nextdoor.domain.post.search.dto.PostBatchResult;
import com.nextdoor.nextdoor.domain.post.search.dto.PostWithLikeCountDto;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class IndexingMessageConsumer implements MessageConsumer {

    private static final int MAX_IN_FLIGHT_TASKS = 2;

    private final PostRepository postRepository;
    private final ElasticsearchClient esClient;
    private final ElasticsearchAsyncClient asyncEsClient;
    private final IndexLockService indexLockService;
    private final PostSearchRepository elasticSearchRepository;
    private final PostBatchReader postBatchReader;
    private final ObjectMapper objectMapper;

    @Override
    public void processMessage(String payload) throws Exception {
        Map<String, Object> message = objectMapper.readValue(payload, Map.class);
        String action = (String) message.get("action");

        if ("REINDEX_ALL".equals(action)) {
            this.reindexAll();
        } else {
            Long postId = ((Number) message.get("postId")).longValue();
            if ("INDEX".equals(action)) {
                this.indexSinglePost(postId);
            } else if ("DELETE".equals(action)) {
                this.deleteSingleIndex(postId);
            } else {
                log.warn("처리할 수 없는 인덱싱 액션 : {}", action);
            }
        }
    }

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
        if (indexLockService.isFullIndexLocked()) {
            throw new PostIndexException("이미 전체 인덱싱 중입니다.");
        }

        elasticSearchRepository.deleteById(postId);
        log.info("단건 삭제 완료: {}", postId);
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