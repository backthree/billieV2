package com.nextdoor.nextdoor.domain.post.search.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
        SELECT id
        FROM outbox_event
        WHERE published = false
        ORDER BY id ASC
        LIMIT 100
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Long> findOutboxIdsToProcess();

    @Modifying
    @Query(value = "UPDATE outbox_event SET published = true WHERE id IN (:ids)", nativeQuery = true)
    int markPublished(@Param("ids") List<Long> ids);
}