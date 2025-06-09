package com.nextdoor.nextdoor.domain.rentalreservation.listener;

import com.nextdoor.nextdoor.domain.rentalreservation.event.RentalReservationConfirmedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReservationConfirmedEventListener {

    private final RentalService rentalService;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReservationConfirmedEvent(RentalReservationConfirmedEvent event) {
        rentalService.createFromRentalReservation(event);
    }
}