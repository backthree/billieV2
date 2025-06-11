package com.nextdoor.nextdoor.domain.rentalreservation.application.port;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.ManagedRentalCountResult;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.RequestRemittanceResult;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.SearchRentalCommand;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.SearchRentalResult;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface RentalQueryPort {

    Page<SearchRentalResult> searchRentals(SearchRentalCommand command);
    Optional<RequestRemittanceResult> findRemittanceRequestViewData(Long rentalId);
    ManagedRentalCountResult countManagedRentals(Long ownerId);
    Optional<SearchRentalResult> findRentalById(Long rentalId);
}
