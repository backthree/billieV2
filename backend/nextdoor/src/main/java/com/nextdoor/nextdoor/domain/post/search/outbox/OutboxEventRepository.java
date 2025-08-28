package com.nextdoor.nextdoor.domain.post.search.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = "SELECT MIN(id) FROM outbox_event WHERE published = false", nativeQuery = true)
    Optional<Long> findMinIdForPublish();

    @Query(value = """
        SELECT
            e.id               AS id,
            e.aggregate_type   AS aggregateType,
            e.aggregate_id     AS aggregateId,
            e.event_type       AS eventType,
            e.payload          AS payload,
            e.version          AS version
        FROM outbox_event e
        WHERE e.published = false
          AND e.id BETWEEN :minId AND :maxId
        ORDER BY e.id ASC
        LIMIT :batch
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEventDto> lockBatchViewsForPublish(@Param("minId") Long minId,
                                                  @Param("maxId") Long maxId,
                                                  @Param("batch") int batch);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE outbox_event
        SET published = true
        WHERE published = false
          AND id BETWEEN :minId AND :maxId
        """, nativeQuery = true)
    int markPublishedRange(@Param("minId") Long minId, @Param("maxId") Long maxId);
}