package com.nextdoor.nextdoor.domain.rentalreservation.port;

import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.ManagedRentalCountResult;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.RequestRemittanceResult;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.SearchRentalCommand;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.SearchRentalResult;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface RentalQueryPort {

    Page<SearchRentalResult> searchRentals(SearchRentalCommand command);
    Optional<RequestRemittanceResult> findRemittanceRequestViewData(Long rentalId);
    ManagedRentalCountResult countManagedRentals(Long ownerId);
    Optional<SearchRentalResult> findRentalById(Long rentalId);
}
