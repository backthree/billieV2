package com.nextdoor.nextdoor.domain.chat.service;

import com.nextdoor.nextdoor.domain.chat.domain.UnreadCounter;
import com.nextdoor.nextdoor.domain.chat.infrastructure.persistence.UnreadCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;

/**
 * Cassandra Counter를 이용한 미확인 메시지 집계 로직
 */
@Service
@RequiredArgsConstructor
public class UnreadCounterService {

    private final MongoTemplate mongoTemplate;
    private final UnreadCounterRepository repository;

    /** 채팅 수신 시 해당 유저의 미확인 카운트 1 증가 */
    public void increase(Long roomId, Long userId) {
        Query query = Query.query(
                Criteria.where("roomId").is(roomId)
                        .and("userId").is(userId)
        );
        Update update = new Update().inc("unreadCount", 1)
                .setOnInsert("roomId", roomId)
                .setOnInsert("userId", userId);
        mongoTemplate.upsert(query, update, UnreadCounter.class);
    }

    /** 사용자가 방 내역 열람 시 미확인 카운트 초기화 */
    public void reset(Long roomId, Long userId) {
        repository.deleteByRoomIdAndUserId(roomId, userId);
    }

    /** 특정 유저의 미확인 메시지 개수 조회 */
    /** 현재 미확인 메시지 개수 조회 */
    public long getCount(Long roomId, Long userId) {
        return repository.findByRoomIdAndUserId(roomId, userId)
                .map(UnreadCounter::getUnreadCount)
                .orElse(0L);
    }
}
