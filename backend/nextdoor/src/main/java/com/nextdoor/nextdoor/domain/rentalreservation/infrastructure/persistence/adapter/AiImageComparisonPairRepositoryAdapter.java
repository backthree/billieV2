package com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.persistence.adapter;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.AiImageComparisonPair;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.repository.AiImageComparisonPairRepository;
import com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.persistence.jpa.AiImageComparisonPairJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AiImageComparisonPairRepositoryAdapter implements AiImageComparisonPairRepository {

    private final AiImageComparisonPairJpaRepository jpaRepository;

    @Override
    public void deleteByRentalId(Long rentalId) {
        jpaRepository.deleteByRentalId(rentalId);
    }

    @Override
    public void save(AiImageComparisonPair aiImageComparisonPair) {
        jpaRepository.save(aiImageComparisonPair);
    }
}
