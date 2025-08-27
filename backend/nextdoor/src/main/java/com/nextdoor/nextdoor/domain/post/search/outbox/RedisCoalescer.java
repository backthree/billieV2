package com.nextdoor.nextdoor.domain.post.search.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
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
        String key = KEY(postId);
        String existing = redisTemplate.opsForValue().get(key);
        if (existing != null) {
            long curVer = jsons.readVersion(existing);
            if (version <= curVer) {
                redisTemplate.expire(key, TTL_SEC, TimeUnit.SECONDS);
                coalescerHit.increment();
                return;
            }
        }
        String store = jsons.wrap(version, payload);
        redisTemplate.opsForValue().set(key, store, TTL_SEC, TimeUnit.SECONDS);
        trackedKeys.put(key, System.currentTimeMillis() + (TTL_SEC * 1000));
        coalescerMiss.increment();
    }

    private String KEY(Long id) {
        return String.format(KEY_FMT, id);
    }

    @Scheduled(fixedDelay = 30000)
    public void flushNearExpiry() {
        long currentTime = System.currentTimeMillis();
        long flushThreshold = 30 * 1000;

        trackedKeys.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            long expiryTime = entry.getValue();

            if (currentTime >= expiryTime - flushThreshold) {
                try {
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl != null && ttl > 0 && ttl <= 30) {
                        String value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            String payload = jsons.readPayload(value);
                            sqsPublisher.sendUpsert(payload);
                            redisTemplate.delete(key);
                            coalescerFlushed.increment();
                            ttlAtFlush.record(ttl);
                            log.info("TTL 만료 임박 key 플러시 완료: {}", key);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Key {} 플러시 중 오류: {}", key, e.getMessage());
                }
                return true;
            }
            return false;
        });
    }
}