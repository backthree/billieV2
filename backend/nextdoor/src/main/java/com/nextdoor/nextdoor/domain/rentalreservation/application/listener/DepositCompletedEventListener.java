package com.nextdoor.nextdoor.domain.rentalreservation.application.listener;

import com.nextdoor.nextdoor.domain.fintech.event.DepositCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.application.service.RentalSettlementService;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.RentalReservationProcess;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.RentalReservationStatus;
import com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.message.RentalStatusMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DepositCompletedEventListener {

    private final RentalSettlementService rentalSettlementService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleDepositCompletedEvent(DepositCompletedEvent event) {
        rentalSettlementService.completeDepositProcessing(event);

        messagingTemplate.convertAndSend(
                "/topic/rental-reservation/" + event.getRentalId() + "/status",
                RentalStatusMessage.builder()
                        .process(RentalReservationProcess.RENTAL_COMPLETED.name())
                        .detailStatus(RentalReservationStatus.RENTAL_COMPLETED.name())
                        .build()
        );
    }
}
