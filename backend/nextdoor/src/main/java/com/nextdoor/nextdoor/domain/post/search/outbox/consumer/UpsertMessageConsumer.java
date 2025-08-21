package com.nextdoor.nextdoor.domain.post.search.outbox.consumer;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdoor.nextdoor.domain.post.search.PostDocument;
import com.nextdoor.nextdoor.domain.post.search.outbox.Jsons;
import com.nextdoor.nextdoor.domain.post.search.outbox.event.PostUpsertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Profile("worker")
@Slf4j
public class UpsertMessageConsumer {
    private final ElasticsearchAsyncClient es;
    private final Jsons jsons;

    private final List<BulkOperation> buffer = Collections.synchronizedList(new ArrayList<>());

    public void process(String msg) {
        try {
            PostUpsertEvent e = jsons.toUpsert(msg);

            PostDocument doc = PostDocument.builder()
                .id(e.getPostId()).title(e.getTitle()).content(e.getContent())
                .rentalFee(e.getRentalFee()).deposit(e.getDeposit())
                .address(e.getAddress())
                .location(e.getLat()!=null&&e.getLon()!=null ? new PostDocument.GeoPoint(e.getLat(), e.getLon()) : null)
                .category(e.getCategory()).likeCount(e.getLikeCount())
                .createdAt(LocalDateTime.parse(e.getCreatedAtIso()))
                .build();

            BulkOperation op = BulkOperation.of(b -> b.index(i -> i
                .index("posts")
                .id(String.valueOf(e.getPostId()))
                .version(e.getVersion())
                .versionType(VersionType.ExternalGte)
                .document(doc)
            ));
            buffer.add(op);
            log.debug("업서트 작업을 버퍼에 추가 (게시물 ID={})", e.getPostId());

            if (buffer.size() >= 500) flush();
        } catch (Exception e) {
            log.error("업서트 메시지 처리 중 오류 발생: {}", msg, e);
        }
    }

    @Scheduled(fixedDelay = 800)
    public void periodicFlush() { 
        if (!buffer.isEmpty()) flush(); 
    }

    private synchronized void flush() {
        if (buffer.isEmpty()) return;
        List<BulkOperation> ops = new ArrayList<>(buffer);
        buffer.clear();
        log.info("업서트 작업 {} 건 플러시 시작", ops.size());
        try {
            es.bulk(b -> b.index("posts").operations(ops)).join();
            log.info("업서트 작업 {} 건 플러시 완료", ops.size());
        } catch (Exception e) {
            log.error("업서트 작업 플러시 중 오류 발생", e);
        }
    }
}
