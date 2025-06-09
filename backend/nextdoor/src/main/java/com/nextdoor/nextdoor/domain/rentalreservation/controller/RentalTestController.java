package com.nextdoor.nextdoor.domain.rentalreservation.controller;

import com.nextdoor.nextdoor.domain.fintech.event.DepositCompletedEvent;
import com.nextdoor.nextdoor.domain.fintech.event.RemittanceCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.service.RentalEndService;
import com.nextdoor.nextdoor.domain.rentalreservation.event.RentalReservationConfirmedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * This controller is for testing purposes only.
 * It simulates events that would normally come from other domains.
 */
@RestController
@RequestMapping("/api/v1/test/rentals")
@RequiredArgsConstructor
public class RentalTestController {

    private final ApplicationEventPublisher eventPublisher;
    private final RentalEndService rentalEndService;

    @Transactional
    @PostMapping("/reservation-confirmed")
    public ResponseEntity<Void> simulateReservationConfirmed(
            @RequestParam Long reservationId,
            @RequestParam LocalDate endDate) {

        RentalReservationConfirmedEvent event = RentalReservationConfirmedEvent.builder()
                .rentalReservationId(reservationId)
                .build();

        eventPublisher.publishEvent(event);

        return ResponseEntity.ok().build();
    }

    //TransactionalEventListener 어노테이션을 사용중이라 트랜잭션 경계 설정
    @Transactional
    @PostMapping("/{rentalId}/remittance-completed")
    public ResponseEntity<Void> simulateRemittanceCompleted(@PathVariable Long rentalId) {
        RemittanceCompletedEvent event = RemittanceCompletedEvent.builder()
                .rentalId(rentalId)
                .build();

        eventPublisher.publishEvent(event);

        return ResponseEntity.ok().build();
    }

    @Transactional
    @PostMapping("/{rentalId}/deposit-completed")
    public ResponseEntity<Void> simulateDepositCompleted(@PathVariable Long rentalId) {
        DepositCompletedEvent event = DepositCompletedEvent.builder()
                .rentalId(rentalId)
                .build();

        eventPublisher.publishEvent(event);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{rentalId}/rental-end")
    public ResponseEntity<Void> simulateRentalEnd(@PathVariable Long rentalId) {
        rentalEndService.rentalEnd(rentalId);

        return ResponseEntity.ok().build();
    }
}
