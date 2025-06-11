package com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.repository;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.entity.AiImageComparisonPair;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiImageComparisonPairRepository extends JpaRepository<AiImageComparisonPair, Long> {

    void deleteByRentalId(Long rentalId);
}
