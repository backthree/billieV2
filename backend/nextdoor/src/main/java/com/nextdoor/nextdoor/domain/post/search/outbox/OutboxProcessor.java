package com.nextdoor.nextdoor.domain.post.search.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
    private final OutboxClaimService claimService;
    private final OutboxEventRepository outboxRepo;
    private final SqsPublisher sqsPublisher;
    private final RedisCoalescer coalescer;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelay = 2000)
    public void pollAndRoute() {
        Timer.Sample totalTimer = Timer.start(meterRegistry);
        Timer.Sample claimTimer = null;
        Timer.Sample fetchTimer = null;
        Timer.Sample processTimer = null;
        Timer.Sample markTimer = null;

        try {
            // ID 조회 단계
            claimTimer = Timer.start(meterRegistry);
            List<Long> ids = claimService.claimIds();
            claimTimer.stop(Timer.builder("outbox.process.step.claim")
                    .description("ID 조회 단계 시간")
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            if (ids.isEmpty()) {
                meterRegistry.counter("outbox.process.empty_batch").increment();
                return;
            }

            // 실제 엔티티 조회 단계
            fetchTimer = Timer.start(meterRegistry);
            List<OutboxEvent> events = outboxRepo.findAllById(ids);
            fetchTimer.stop(Timer.builder("outbox.process.step.fetch")
                    .description("엔티티 조회 단계 시간")
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            // 이벤트 처리 단계
            processTimer = Timer.start(meterRegistry);
            int deleteCount = 0;
            int upsertCount = 0;
            int errorCount = 0;

            for (OutboxEvent e : events) {
                Timer.Sample eventTimer = Timer.start(meterRegistry);
                try {
                    if ("DELETE".equals(e.getEventType())) {
                        sqsPublisher.sendDelete(e.getPayload()).join();
                        deleteCount++;
                        eventTimer.stop(Timer.builder("outbox.process.event.delete")
                                .description("DELETE 이벤트 처리 시간")
                                .publishPercentileHistogram()
                                .register(meterRegistry));
                    } else {
                        coalescer.put(e.getAggregateId(), e.getVersion(), e.getPayload());
                        upsertCount++;
                        eventTimer.stop(Timer.builder("outbox.process.event.upsert")
                                .description("UPSERT 이벤트 처리 시간")
                                .publishPercentileHistogram()
                                .register(meterRegistry));
                    }
                } catch (Exception ex) {
                    errorCount++;
                    eventTimer.stop(Timer.builder("outbox.process.event.error")
                            .description("이벤트 처리 에러 시간")
                            .publishPercentileHistogram()
                            .register(meterRegistry));
                    log.error("Outbox 처리 실패 id={}", e.getId(), ex);
                }
            }

            processTimer.stop(Timer.builder("outbox.process.step.process")
                    .description("이벤트 처리 단계 시간")
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            // 발행 마킹 단계
            markTimer = Timer.start(meterRegistry);
            claimService.markPublished(ids);
            markTimer.stop(Timer.builder("outbox.process.step.mark")
                    .description("발행 마킹 단계 시간 (현재 IN절)")
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            meterRegistry.counter("outbox.process.events", "type", "delete").increment(deleteCount);
            meterRegistry.counter("outbox.process.events", "type", "upsert").increment(upsertCount);
            meterRegistry.counter("outbox.process.events", "type", "error").increment(errorCount);
            meterRegistry.gauge("outbox.process.batch_size", ids.size());

            log.info("아웃박스 {}건 처리 완료 (DELETE:{}, UPSERT:{}, ERROR:{})",
                    ids.size(), deleteCount, upsertCount, errorCount);

        } catch (Exception e) {
            meterRegistry.counter("outbox.process.fatal_error").increment();
            log.error("Outbox 처리 중 치명적 에러", e);
        } finally {
            totalTimer.stop(Timer.builder("outbox.process.total")
                    .description("전체 배치 처리 시간")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
}