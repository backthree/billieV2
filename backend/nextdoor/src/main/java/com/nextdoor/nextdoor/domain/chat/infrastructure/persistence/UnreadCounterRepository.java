package com.nextdoor.nextdoor.domain.chat.infrastructure.persistence;

import com.nextdoor.nextdoor.domain.chat.domain.UnreadCounter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnreadCounterRepository
        extends MongoRepository<UnreadCounter, String> {

    Optional<UnreadCounter> findByRoomIdAndUserId(Long roomId, Long userId);
    void deleteByRoomIdAndUserId(Long roomId, Long userId);
}
