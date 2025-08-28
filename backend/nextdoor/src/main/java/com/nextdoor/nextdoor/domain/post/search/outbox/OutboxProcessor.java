package com.nextdoor.nextdoor.domain.post.search.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
    private final OutboxClaimService claimService;
    private final OutboxService outboxService;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelay = 2000)
    public void pollAndRoute() {
        Timer.Sample totalTimer = Timer.start(meterRegistry);
        Timer.Sample claimTimer = null;
        Timer.Sample processTimer = null;
        Timer.Sample markTimer = null;

        try {
            // ID/뷰 조회
            claimTimer = Timer.start(meterRegistry);
            List<OutboxEventDto> views = claimService.claimViews();
            claimTimer.stop(Timer.builder("outbox.process.step.claim")
                    .description("ID 조회 단계 (projection)")
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            if (views.isEmpty()) {
                meterRegistry.counter("outbox.process.empty_batch").increment();
                return;
            }

            // 정렬 보장
            views.sort(Comparator.comparingLong(OutboxEventDto::getId));

            // 처리 (SQS/Redis)
            processTimer = Timer.start(meterRegistry);
            outboxService.publishViews(views);
            processTimer.stop(Timer.builder("outbox.process.step.process")
                    .description("이벤트 처리 단계 (projection)")
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            long minId = views.get(0).getId();
            long maxId = views.get(views.size() - 1).getId();

            markTimer = Timer.start(meterRegistry);
            claimService.markPublishedByRange(minId, maxId);
            markTimer.stop(Timer.builder("outbox.process.step.mark")
                    .description("발행 마킹 단계 (BETWEEN)")
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            meterRegistry.gauge("outbox.process.batch_size", views.size());

            log.info("아웃박스 {}건 처리 완료 (id {}..{})", views.size(), minId, maxId);

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