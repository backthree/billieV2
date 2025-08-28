package com.nextdoor.nextdoor.domain.post.search.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxRepo;
    private final MeterRegistry meterRegistry;

    @Transactional
    public List<OutboxEventDto> claimViews() {
        Timer.Sample overall = Timer.start(meterRegistry);

        try {
            Timer.Sample claimTimer = Timer.start(meterRegistry);
            var minOpt = outboxRepo.findMinIdForPublish();
            claimTimer.stop(Timer.builder("outbox.process.step.claim")
                    .description("ID 조회 단계 (MIN(id))")
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            if (minOpt.isEmpty()) {
                meterRegistry.counter("outbox.claim.count", "result", "success").increment(0);
                return Collections.emptyList();
            }

            long minId = minOpt.get();
            long maxId = minId + BATCH_SIZE - 1;

            Timer.Sample fetchTimer = Timer.start(meterRegistry);
            List<OutboxEventDto> views = outboxRepo.lockBatchViewsForPublish(minId, maxId, BATCH_SIZE);
            fetchTimer.stop(Timer.builder("outbox.process.step.fetch")
                    .description("엔티티(프로젝션) 잠금 조회 단계")
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            meterRegistry.counter("outbox.claim.count", "result", "success").increment(views.size());
            return views;

        } catch (Exception e) {
            meterRegistry.counter("outbox.claim.count", "result", "error").increment();
            throw e;
        } finally {
            overall.stop(Timer.builder("outbox.claim.duration")
                    .description("Outbox ID/뷰 클레임 전체 시간 (projection)")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    @Transactional
    public int markPublishedByRange(long minId, long maxId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            int updated = outboxRepo.markPublishedRange(minId, maxId);
            meterRegistry.counter("outbox.mark_published.count", "result", "success").increment(updated);
            return updated;
        } catch (Exception e) {
            meterRegistry.counter("outbox.mark_published.count", "result", "error").increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("outbox.mark_published.duration")
                    .description("Outbox 발행 마킹 시간 (BETWEEN)")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
}