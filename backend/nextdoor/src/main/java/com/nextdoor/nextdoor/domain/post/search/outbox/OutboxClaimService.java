package com.nextdoor.nextdoor.domain.post.search.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private final OutboxEventRepository outboxRepo;
    private final MeterRegistry meterRegistry;

    @Transactional
    public List<Long> claimIds() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<Long> ids = outboxRepo.findOutboxIdsToProcess();
            meterRegistry.counter("outbox.claim.count", "result", "success")
                    .increment(ids.size());
            return ids;
        } catch (Exception e) {
            meterRegistry.counter("outbox.claim.count", "result", "error").increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("outbox.claim.duration")
                    .description("Outbox ID 조회 시간")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    @Transactional
    public void markPublished(List<Long> ids) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            int updatedRows = outboxRepo.markPublished(ids);
            meterRegistry.counter("outbox.mark_published.count", "result", "success")
                    .increment(updatedRows);
        } catch (Exception e) {
            meterRegistry.counter("outbox.mark_published.count", "result", "error").increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("outbox.mark_published.duration")
                    .description("Outbox 발행 마킹 시간 (IN절)")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
}