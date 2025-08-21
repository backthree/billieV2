package com.nextdoor.nextdoor.domain.post.search.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCoalescer {
    private final String KEY_FMT = "idx:post:%d";
    private final RedisTemplate<String, String> redisTemplate;
    private final SqsPublisher sqsPublisher;
    private final Jsons jsons;

    private static final long TTL_SEC = 180;

    public void put(Long postId, Long version, String payload) {
        String key = KEY(postId);
        String existing = redisTemplate.opsForValue().get(key);
        if (existing != null) {
            long curVer = jsons.readVersion(existing);
            if (version <= curVer) {
                redisTemplate.expire(key, TTL_SEC, TimeUnit.SECONDS);
                return;
            }
        }
        String store = jsons.wrap(version, payload);
        redisTemplate.opsForValue().set(key, store, TTL_SEC, TimeUnit.SECONDS);
    }

    private String KEY(Long id) {
        return String.format(KEY_FMT, id);
    }

    // 30초마다 스캔 TTL이 0~30초 남은 키를 찾아 발행
    @Scheduled(fixedDelay = 30000)
    public void flushNearExpiry() {
        Set<String> keys = redisTemplate.keys("idx:post:*");
        if (keys == null) return;
        for (String k : keys) {
            Long ttl = redisTemplate.getExpire(k);
            if (ttl != null && ttl <= 30) {
                String v = redisTemplate.opsForValue().get(k);
                if (v != null) {
                    String payload = jsons.readPayload(v);
                    sqsPublisher.sendUpsert(payload);
                    redisTemplate.delete(k);
                    log.info("TTL 만료 임박 key 플러시 완료: {}", k);
                }
            }
        }
    }
}
