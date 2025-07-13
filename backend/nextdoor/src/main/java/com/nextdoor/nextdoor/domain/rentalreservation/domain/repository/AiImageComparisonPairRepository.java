package com.nextdoor.nextdoor.domain.rentalreservation.domain.repository;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.AiImageComparisonPair;

public interface AiImageComparisonPairRepository{

    void deleteByRentalId(Long rentalId);
    void save(AiImageComparisonPair aiImageComparisonPair);
}
