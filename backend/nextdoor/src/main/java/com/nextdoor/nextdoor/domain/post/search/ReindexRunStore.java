package com.nextdoor.nextdoor.domain.post.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReindexRunStore {
    private static final String KEY = "REINDEX_RUN_STATE";
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper om;

    public Optional<ReindexRunState> get() {
        try {
            String json = redisTemplate.opsForValue().get(KEY);
            return json == null ? Optional.empty() : Optional.of(om.readValue(json, ReindexRunState.class));
        } catch (Exception e) {
            throw new IllegalStateException("런 상태 로드 실패", e);
        }
    }

    public ReindexRunState begin(Instant cutoff) {
        ReindexRunState s = ReindexRunState.builder()
                .runId(UUID.randomUUID().toString())
                .cutoff(cutoff)
                .lastId(0L)
                .processed(0L)
                .status(ReindexRunState.Status.RUNNING)
                .startedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        save(s);
        return s;
    }

    public void checkpoint(long lastId, long processed) {
        ReindexRunState s = get().orElseThrow();
        s.setLastId(lastId);
        s.setProcessed(processed);
        s.setUpdatedAt(Instant.now());
        save(s);
    }

    public void complete() {
        ReindexRunState s = get().orElseThrow();
        s.setStatus(ReindexRunState.Status.COMPLETED);
        s.setUpdatedAt(Instant.now());
        save(s);
    }

    public void abort(String reason) {
        ReindexRunState s = get().orElseThrow();
        s.setStatus(ReindexRunState.Status.ABORTED);
        s.setAbortReason(reason);
        s.setUpdatedAt(Instant.now());
        save(s);
    }

    private void save(ReindexRunState s) {
        try {
            redisTemplate.opsForValue().set(KEY, om.writeValueAsString(s));
        } catch (Exception e) {
            throw new IllegalStateException("런 상태 저장 실패", e);
        }
    }

    public void clear() {
        redisTemplate.delete(KEY);
    }
}