package com.nextdoor.nextdoor.domain.rentalreservation.listener;

import com.nextdoor.nextdoor.domain.fintech.event.DepositCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationProcess;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationStatus;
import com.nextdoor.nextdoor.domain.rentalreservation.message.RentalStatusMessage;
import com.nextdoor.nextdoor.domain.rentalreservation.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DepositCompletedEventListener {

    private final RentalService rentalService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleDepositCompletedEvent(DepositCompletedEvent event) {
        rentalService.completeDepositProcessing(event);

        messagingTemplate.convertAndSend(
                "/topic/rental-reservation/" + event.getRentalId() + "/status",
                RentalStatusMessage.builder()
                        .process(RentalReservationProcess.RENTAL_COMPLETED.name())
                        .detailStatus(RentalReservationStatus.RENTAL_COMPLETED.name())
                        .build()
        );
    }
}
