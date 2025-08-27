package com.nextdoor.nextdoor.domain.post.search.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {
    
    private final OutboxEventRepository outboxRepo;

    @Transactional(readOnly = false)
    public List<Long> claimIds() {
        return outboxRepo.findOutboxIdsToProcess();
    }

    @Transactional
    public void markPublished(List<Long> ids) {
        outboxRepo.markPublished(ids);
    }
}
