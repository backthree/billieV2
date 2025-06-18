package com.nextdoor.nextdoor.domain.rentalreservation.presentation.controller;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.DeleteRentalCommand;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.DeleteRentalResult;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.ManagedRentalCountResult;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.SearchRentalCommand;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.SearchRentalResult;
import com.nextdoor.nextdoor.domain.rentalreservation.application.service.RentalQueryService;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.request.RetrieveRentalsRequest;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.DeleteRentalResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.ManagedRentalCountResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.RentalDetailResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.mapper.RentalReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
public class RentalQueryController {

    private final RentalQueryService rentalQueryService;
    private final RentalReservationMapper rentalReservationMapper;

    @GetMapping
    public ResponseEntity<Page<RentalDetailResponse>> getMyRentals(
            @RequestParam Long userId,
            @RequestParam String userRole,
            @RequestParam String condition,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        RetrieveRentalsRequest retrieveRentalsRequest = new RetrieveRentalsRequest(userRole, condition);
        SearchRentalCommand command = rentalReservationMapper.toCommand(userId, retrieveRentalsRequest, pageable);
        Page<SearchRentalResult> results = rentalQueryService.searchRentals(command);
        Page<RentalDetailResponse> responsePage = results.map(rentalReservationMapper::toResponse);

        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/managed-count")
    public ResponseEntity<ManagedRentalCountResponse> getManagedRentalCount(
            @RequestParam Long userId) {

        ManagedRentalCountResult result = rentalQueryService.countManagedRentals(userId);
        ManagedRentalCountResponse response = rentalReservationMapper.toManagedRentalCountResponse(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rentalId}")
    public ResponseEntity<RentalDetailResponse> getRentalById(
            @PathVariable Long rentalId) {

        SearchRentalResult result = rentalQueryService.getRentalById(rentalId);
        RentalDetailResponse response = rentalReservationMapper.toResponse(result);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{rentalId}")
    public ResponseEntity<DeleteRentalResponse> deleteRental(
            @PathVariable Long rentalId) {

        DeleteRentalCommand command = rentalReservationMapper.toDeleteCommand(rentalId);
        DeleteRentalResult result = rentalQueryService.deleteRental(command);
        DeleteRentalResponse response = rentalReservationMapper.toDeleteResponse(result);

        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}