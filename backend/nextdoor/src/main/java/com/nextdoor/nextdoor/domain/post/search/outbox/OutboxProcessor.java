package com.nextdoor.nextdoor.domain.post.search.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxEventRepository outboxRepo;
    private final SqsPublisher sqsPublisher;
    private final RedisCoalescer coalescer;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelay = 2000)
    public void pollAndRoute() {
        Timer.Sample total = Timer.start(meterRegistry);

        List<Long> ids;
        try {
            ids = claimIds();
            if (ids.isEmpty()) {
                return;
            }
        } catch (Exception e) {
            log.error("Outbox ID 클레임 실패", e);
            return;
        }

        try {
            List<OutboxEvent> events = outboxRepo.findAllById(ids);
            for (OutboxEvent e : events) {
                try {
                    if ("DELETE".equals(e.getEventType())) {
                        sqsPublisher.sendDelete(e.getPayload()).join();
                    } else {
                        coalescer.put(e.getAggregateId(), e.getVersion(), e.getPayload());
                    }
                } catch (Exception ex) {
                    log.error("Outbox 처리 실패 id={}", e.getId(), ex);
                }
            }

            outboxRepo.markPublished(ids);

            log.info("아웃박스 {}건 처리 완료", ids.size());
        } finally {
            total.stop(Timer.builder("outbox.batch.total")
                    .description("아웃박스 배치 처리 시간")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    @Transactional
    public List<Long> claimIds() {
        return outboxRepo.findOutboxIdsToProcess();
    }
}