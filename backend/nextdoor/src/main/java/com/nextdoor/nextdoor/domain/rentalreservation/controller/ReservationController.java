package com.nextdoor.nextdoor.domain.rentalreservation.controller;

import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request.*;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response.ReservationResponseDto;
import com.nextdoor.nextdoor.domain.rentalreservation.service.ReservationService;
import com.nextdoor.nextdoor.domain.rentalreservation.service.RentalService;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.SearchRentalCommand;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final RentalService rentalService;

    @PostMapping
    public ResponseEntity<ReservationResponseDto> createReservation(
            HttpServletRequest request,
            @AuthenticationPrincipal Long loginUserId,
            @RequestBody ReservationSaveRequestDto reservationSaveRequestDto
    ) {
        ReservationResponseDto reservationResponseDto = reservationService.createReservation(loginUserId, reservationSaveRequestDto);
        return ResponseEntity.created(URI.create(request.getRequestURI())).body(reservationResponseDto);
    }

    @PutMapping("/{reservationId}")
    public ResponseEntity<ReservationResponseDto> updateReservation(
            @AuthenticationPrincipal Long loginUserId,
            @PathVariable Long reservationId,
            @RequestBody ReservationUpdateRequestDto reservationUpdateRequestDto
    ) {
        ReservationResponseDto reservationResponseDto = reservationService.updateReservation(loginUserId, reservationId, reservationUpdateRequestDto);
        return ResponseEntity.ok(reservationResponseDto);
    }

    @PatchMapping("/{reservationId}/status")
    public ResponseEntity<ReservationResponseDto> updateReservationStatus(
            @AuthenticationPrincipal Long loginUserId,
            @PathVariable Long reservationId,
            @RequestBody ReservationStatusUpdateRequestDto reservationStatusUpdateRequestDto
    ) {
        reservationService.confirmReservation(loginUserId, reservationId, reservationStatusUpdateRequestDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ReservationResponseDto> deleteReservation(
            @AuthenticationPrincipal Long loginUserId,
            @PathVariable Long reservationId
    ) {
        reservationService.deleteReservation(loginUserId, reservationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sent")
    public ResponseEntity<Page<?>> retrieveSentReservations(
            @AuthenticationPrincipal Long loginUserId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        SearchRentalCommand command = SearchRentalCommand.builder()
                .userId(loginUserId)
                .userRole("RENTER")
                .condition("ACTIVE")
                .pageable(pageable)
                .build();
        
        return ResponseEntity.ok(rentalService.searchRentals(command));
    }

    @GetMapping("/received")
    public ResponseEntity<Page<?>> retrieveReceivedReservations(
            @AuthenticationPrincipal Long loginUserId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        SearchRentalCommand command = SearchRentalCommand.builder()
                .userId(loginUserId)
                .userRole("OWNER")
                .condition("ACTIVE")
                .pageable(pageable)
                .build();
        
        return ResponseEntity.ok(rentalService.searchRentals(command));
    }
}