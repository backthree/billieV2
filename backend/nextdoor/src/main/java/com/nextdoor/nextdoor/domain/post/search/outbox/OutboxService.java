package com.nextdoor.nextdoor.domain.post.search.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    private final SqsPublisher sqsPublisher;
    private final RedisCoalescer coalescer;
    private final MeterRegistry meterRegistry;

    public void publishViews(List<OutboxEventDto> batch) {
        for (OutboxEventDto e : batch) {
            Timer.Sample eventTimer = Timer.start(meterRegistry);
            try {
                if ("DELETE".equals(e.getEventType())) {
                    sqsPublisher.sendDelete(e.getPayload()).join();
                    eventTimer.stop(Timer.builder("outbox.process.event.delete")
                            .description("DELETE 이벤트 처리 시간")
                            .publishPercentileHistogram()
                            .register(meterRegistry));
                } else {
                    coalescer.put(e.getAggregateId(), e.getVersion(), e.getPayload());
                    eventTimer.stop(Timer.builder("outbox.process.event.upsert")
                            .description("UPSERT 이벤트 처리 시간")
                            .publishPercentileHistogram()
                            .register(meterRegistry));
                }
            } catch (Exception ex) {
                eventTimer.stop(Timer.builder("outbox.process.event.error")
                        .description("이벤트 처리 에러 시간")
                        .publishPercentileHistogram()
                        .register(meterRegistry));
                log.error("Outbox 이벤트 처리 실패 id={}", e.getId(), ex);
            }
        }
    }
}