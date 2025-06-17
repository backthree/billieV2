package com.nextdoor.nextdoor.domain.rentalreservation.application.service;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * 대여 조회 서비스
 * 책임: 대여 예약 정보 조회 (읽기 전용) 작업을 담당합니다.
 */
@Service
public interface RentalQueryService {

    /**
     * 대여 목록을 검색합니다.
     */
    Page<SearchRentalResult> searchRentals(SearchRentalCommand command);

    /**
     * ID로 대여 정보를 조회합니다.
     */
    SearchRentalResult getRentalById(Long rentalId);

    /**
     * 관리 중인 대여 수를 조회합니다.
     */
    ManagedRentalCountResult countManagedRentals(Long ownerId);
    
    /**
     * 대여 정보를 삭제합니다.
     */
    DeleteRentalResult deleteRental(DeleteRentalCommand command);
}