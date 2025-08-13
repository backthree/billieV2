package com.nextdoor.nextdoor.domain.post.search.messaging.consumer;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch._types.VersionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdoor.nextdoor.domain.post.exception.PostIndexException;
import com.nextdoor.nextdoor.domain.post.repository.PostRepository;
import com.nextdoor.nextdoor.domain.post.search.*;
import com.nextdoor.nextdoor.domain.post.search.dto.BatchTask;
import com.nextdoor.nextdoor.domain.post.search.dto.PostBatchResult;
import com.nextdoor.nextdoor.domain.post.search.dto.PostWithLikeCountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class IndexingMessageConsumer implements MessageConsumer {

    private static final int MAX_IN_FLIGHT_TASKS = 2;

    private final PostRepository postRepository;
    private final ElasticsearchAsyncClient asyncEsClient;
    private final IndexLockService indexLockService;
    private final PostSearchRepository elasticSearchRepository;
    private final PostBatchReader postBatchReader;
    private final ObjectMapper objectMapper;
    private final ReindexRunStore runStore;
    private final IndexAliasManager indexAliasManager;

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

        String newIndex = null;
        try {
            Optional<ReindexRunState> runState = runStore.get()
                    .filter(s -> s.getStatus() == ReindexRunState.Status.RUNNING);

            ReindexRunState state = runState.orElseGet(() -> {
                LocalDateTime dbNow = postRepository.currentTimestamp();
                return runStore.begin(dbNow.atZone(ZoneId.systemDefault()).toInstant());
            });

            long lastId = state.getLastId();
            long processed = state.getProcessed();
            LocalDateTime cutoff = LocalDateTime.ofInstant(state.getCutoff(), ZoneId.systemDefault());

            newIndex = indexAliasManager.prepareNewIndex();

            List<BatchTask> inFlight = new ArrayList<>(MAX_IN_FLIGHT_TASKS);

            while (true) {
                PostBatchResult result = postBatchReader.findNextBatch(lastId, cutoff);
                List<PostWithLikeCountDto> postDtos = result.getPosts();
                if (postDtos.isEmpty()) break;

                List<BulkOperation> ops = new ArrayList<>(postDtos.size());
                for (PostWithLikeCountDto dto : postDtos) {
                    PostDocument doc = toDocument(dto);
                    String finalNewIndex = newIndex;
                    long version = toVersion(dto);
                    ops.add(BulkOperation.of(b -> b.index(idx -> idx
                            .index(finalNewIndex)
                            .id(dto.getPostId().toString())
                            .version(version)
                            .versionType(VersionType.ExternalGte)
                            .document(doc))));
                }

                CompletableFuture<Void> f = bulkIndexAsync(ops);
                inFlight.add(new BatchTask(f, result.getLastId(), postDtos.size()));
                lastId = result.getLastId();

                if (inFlight.size() >= MAX_IN_FLIGHT_TASKS) {
                    BatchTask done = inFlight.remove(0);
                    done.future().join();
                    processed += done.count();
                    runStore.checkpoint(done.lastId(), processed);
                }
            }

            while (!inFlight.isEmpty()) {
                BatchTask done = inFlight.remove(0);
                done.future().join();
                processed += done.count();
                runStore.checkpoint(done.lastId(), processed);
            }

            indexAliasManager.finalizeAndSwap(newIndex, "1", "1s");

            runStore.complete();
            log.info("전체 인덱싱 완료 및 스왑 완료: newIndex={}, cutoff={}, processed={}",
                    newIndex, state.getCutoff(), processed);

        } catch (Exception e) {
            log.warn("새 인덱스 {} 로 적재/스왑 중 오류 (스왑 전이면 서비스 검색 영향 없음)", newIndex, e);
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
            long version = toVersion(dto);
            asyncEsClient.index(i -> i
                    .index("posts")
                    .id(dto.getPostId().toString())
                    .version(version)
                    .versionType(VersionType.ExternalGte)
                    .document(doc)
            ).join();

            log.info("단건 색인 완료: id={}, version={}", doc.getId(), version);
        }
    }

    @Transactional
    public void deleteSingleIndex(Long postId) {
        if (indexLockService.isFullIndexLocked()) {
            throw new PostIndexException("이미 전체 인덱싱 중입니다.");
        }

        Long versionFromMsg = null;
        if (versionFromMsg != null) {
            asyncEsClient.delete(d -> d
                    .index("posts")
                    .id(postId.toString())
                    .version(versionFromMsg)
                    .versionType(VersionType.ExternalGte)
            ).join();
            log.info("단건 삭제 완료: id={}, version={}", postId, versionFromMsg);
        } else {
            elasticSearchRepository.deleteById(postId);
            log.info("단건 삭제: {}", postId);
        }
    }

    private CompletableFuture<Void> bulkIndexAsync(List<BulkOperation> operations) {
        BulkRequest bulkRequest = BulkRequest.of(b -> b.operations(operations));
        return asyncEsClient.bulk(bulkRequest)
                .thenAccept(response -> {
                    if (response.errors()) {
                        var harmful = response.items().stream()
                                .filter(item -> item.error() != null)
                                .filter(item -> {
                                    int status = item.status();
                                    String type = item.error().type();
                                    boolean benign409 = status == 409 || "version_conflict_engine_exception".equals(type);
                                    boolean benign404 = status == 404;
                                    return !(benign409 || benign404);
                                })
                                .toList();

                        if (!harmful.isEmpty()) {
                            log.error("Bulk 색인 유해 오류(size={}): {}",
                                    harmful.size(),
                                    harmful.stream().map(i -> i.error().reason()).toList());
                        } else {
                            log.info("Bulk 색인 완료(무해 충돌/없음 제외): {}", operations.size());
                        }
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

    private long toVersion(PostWithLikeCountDto dto) {
        return dto.getUpdatedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
}