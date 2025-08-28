package com.nextdoor.nextdoor.domain.post.search.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCoalescer {
    private final String KEY_FMT = "idx:post:%d";
    private final RedisTemplate<String, String> redisTemplate;
    private final SqsPublisher sqsPublisher;
    private final Jsons jsons;
    private final MeterRegistry meterRegistry;

    private static final long TTL_SEC = 180;
    private final ConcurrentMap<String, Long> trackedKeys = new ConcurrentHashMap<>();

    private Counter coalescerHit;
    private Counter coalescerMiss;
    private Counter coalescerFlushed;
    private DistributionSummary ttlAtFlush;

    @PostConstruct
    public void init() {
        this.coalescerHit = Counter.builder("coalescer.hit")
                .description("이미 존재하여 디바운스된 횟수").tag("entity","post").register(meterRegistry);
        this.coalescerMiss = Counter.builder("coalescer.miss")
                .description("새로 세팅된 횟수").tag("entity","post").register(meterRegistry);
        this.coalescerFlushed = Counter.builder("coalescer.flush")
                .description("TTL 임박 플러시 횟수").tag("entity","post").register(meterRegistry);
        this.ttlAtFlush = DistributionSummary.builder("coalescer.flush.ttl_seconds")
                .description("플러시 직전 남은 TTL(초)").baseUnit("seconds")
                .publishPercentileHistogram().register(meterRegistry);
    }

    public void put(Long postId, Long version, String payload) {
        Timer.Sample totalTimer = Timer.start(meterRegistry);
        Timer.Sample redisGetTimer = null;
        Timer.Sample redisSetTimer = null;
        Timer.Sample redisExpireTimer = null;

        try {
            String key = KEY(postId);

            // Redis GET 연산 모니터링
            redisGetTimer = Timer.start(meterRegistry);
            String existing = redisTemplate.opsForValue().get(key);
            redisGetTimer.stop(Timer.builder("redis.coalescer.get")
                    .description("Redis GET 연산 시간")
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            if (existing != null) {
                long curVer = jsons.readVersion(existing);
                if (version <= curVer) {
                    // Redis EXPIRE 연산 모니터링
                    redisExpireTimer = Timer.start(meterRegistry);
                    redisTemplate.expire(key, TTL_SEC, TimeUnit.SECONDS);
                    redisExpireTimer.stop(Timer.builder("redis.coalescer.expire")
                            .description("Redis EXPIRE 연산 시간")
                            .publishPercentileHistogram()
                            .register(meterRegistry));

                    coalescerHit.increment();
                    return;
                }
            }

            // Redis SET 연산 모니터링
            String store = jsons.wrap(version, payload);
            redisSetTimer = Timer.start(meterRegistry);
            redisTemplate.opsForValue().set(key, store, TTL_SEC, TimeUnit.SECONDS);
            redisSetTimer.stop(Timer.builder("redis.coalescer.set")
                    .description("Redis SET 연산 시간")
                    .publishPercentileHistogram()
                    .register(meterRegistry));

            trackedKeys.put(key, System.currentTimeMillis() + (TTL_SEC * 1000));
            coalescerMiss.increment();

        } finally {
            totalTimer.stop(Timer.builder("redis.coalescer.put.total")
                    .description("Redis Coalescer PUT 전체 시간")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    private String KEY(Long id) {
        return String.format(KEY_FMT, id);
    }

    @Scheduled(fixedDelay = 30000)
    public void flushNearExpiry() {
        Timer.Sample totalTimer = Timer.start(meterRegistry);

        long currentTime = System.currentTimeMillis();
        long flushThreshold = 30 * 1000;

        int processedCount = 0;
        int flushedCount = 0;
        int errorCount = 0;

        try {
            meterRegistry.gauge("coalescer.tracked_keys.size", trackedKeys.size());

            var iterator = trackedKeys.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                String key = entry.getKey();
                long expiryTime = entry.getValue();
                processedCount++;

                if (currentTime >= expiryTime - flushThreshold) {
                    Timer.Sample flushTimer = Timer.start(meterRegistry);
                    try {
                        // Redis GET_EXPIRE 연산 모니터링
                        Timer.Sample ttlTimer = Timer.start(meterRegistry);
                        Long ttl = redisTemplate.getExpire(key);
                        ttlTimer.stop(Timer.builder("redis.coalescer.get_expire")
                                .description("Redis GET_EXPIRE 연산 시간")
                                .publishPercentileHistogram()
                                .register(meterRegistry));

                        if (ttl != null && ttl > 0 && ttl <= 30) {
                            // Redis GET 연산 모니터링
                            Timer.Sample getTimer = Timer.start(meterRegistry);
                            String value = redisTemplate.opsForValue().get(key);
                            getTimer.stop(Timer.builder("redis.coalescer.flush.get")
                                    .description("플러시 시 Redis GET 연산 시간")
                                    .publishPercentileHistogram()
                                    .register(meterRegistry));

                            if (value != null) {
                                // SQS 전송 모니터링
                                Timer.Sample sqsTimer = Timer.start(meterRegistry);
                                String payload = jsons.readPayload(value);
                                sqsPublisher.sendUpsert(payload);
                                sqsTimer.stop(Timer.builder("redis.coalescer.sqs_send")
                                        .description("플러시 시 SQS 전송 시간")
                                        .publishPercentileHistogram()
                                        .register(meterRegistry));

                                // Redis DELETE 연산 모니터링
                                Timer.Sample deleteTimer = Timer.start(meterRegistry);
                                redisTemplate.delete(key);
                                deleteTimer.stop(Timer.builder("redis.coalescer.delete")
                                        .description("Redis DELETE 연산 시간")
                                        .publishPercentileHistogram()
                                        .register(meterRegistry));

                                coalescerFlushed.increment();
                                ttlAtFlush.record(ttl);
                                flushedCount++;
                                log.info("TTL 만료 임박 key 플러시 완료: {}", key);
                            }
                        }
                    } catch (Exception e) {
                        errorCount++;
                        meterRegistry.counter("coalescer.flush.error").increment();
                        log.warn("Key {} 플러시 중 오류: {}", key, e.getMessage());
                    } finally {
                        flushTimer.stop(Timer.builder("redis.coalescer.flush.single")
                                .description("단일 키 플러시 시간")
                                .publishPercentileHistogram()
                                .register(meterRegistry));
                    }
                    iterator.remove();
                }
            }

            // 배치 통계 메트릭
            meterRegistry.counter("coalescer.flush.batch.processed").increment(processedCount);
            meterRegistry.counter("coalescer.flush.batch.flushed").increment(flushedCount);
            meterRegistry.counter("coalescer.flush.batch.errors").increment(errorCount);

            log.info("플러시 배치 완료 - 처리:{}, 플러시:{}, 에러:{}", processedCount, flushedCount, errorCount);

        } finally {
            totalTimer.stop(Timer.builder("redis.coalescer.flush.batch.total")
                    .description("플러시 배치 전체 시간")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
}