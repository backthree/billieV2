package com.nextdoor.nextdoor.domain.post.search.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import jakarta.annotation.PostConstruct;
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
    private final MeterRegistry meterRegistry;
    private Counter batchCount;

    @PostConstruct
    public void init() {
        this.batchCount = Counter.builder("outbox.batch.count")
                .description("아웃박스 배치 처리 횟수")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void pollAndRoute() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<OutboxEvent> batch = outboxRepo.findBatchForPublish(PageRequest.of(0, 500));
            if (batch.isEmpty()) return;

            log.info("아웃박스 이벤트 {} 건 처리 시작", batch.size());
            batchCount.increment();

            for (OutboxEvent e : batch) {
                if ("DELETE".equals(e.getEventType())) {
                    sqsPublisher.sendDelete(e.getPayload());
                } else {
                    coalescer.put(e.getAggregateId(), e.getVersion(), e.getPayload());
                }
                e.setPublished(true);
            }
            outboxRepo.saveAll(batch);
            log.info("아웃박스 이벤트 {} 건 처리 완료", batch.size());
        } catch (Exception e) {
            log.error("아웃박스 이벤트 처리 중 오류 발생", e);
        } finally {
            sample.stop(Timer.builder("outbox.batch.latency")
                    .description("아웃박스 배치 처리 시간")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
}