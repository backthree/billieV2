package com.nextdoor.nextdoor.domain.post.search.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
    private final OutboxEventRepository outboxRepo;
    private final SqsPublisher sqsPublisher;
    private final RedisCoalescer coalescer;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void pollAndRoute() {
        try {
            List<OutboxEvent> batch = outboxRepo.findBatchForPublish(PageRequest.of(0, 500));
            if (batch.isEmpty()) return;

            log.info("아웃박스 이벤트 {} 건 처리 시작", batch.size());

            for (OutboxEvent e : batch) {
                if ("DELETE".equals(e.getEventType())) {
                    sqsPublisher.sendDelete(e.getPayload());
                    e.setPublished(true);
                    log.debug("삭제 이벤트 전송 완료 (게시물 ID={})", e.getAggregateId());
                } else {
                    coalescer.put(e.getAggregateId(), e.getVersion(), e.getPayload());
                    e.setPublished(true);
                    log.debug("업서트 이벤트 병합 완료 (게시물 ID={})", e.getAggregateId());
                }
            }
            outboxRepo.saveAll(batch);
            log.info("아웃박스 이벤트 {} 건 처리 완료", batch.size());
        } catch (Exception e) {
            log.error("아웃박스 이벤트 처리 중 오류 발생", e);
        }
    }
}
