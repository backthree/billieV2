package com.nextdoor.nextdoor.domain.rentalreservation.listener;

import com.nextdoor.nextdoor.domain.fintech.event.RemittanceCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RemittanceCompletedEventListener {

    private final RentalService rentalService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleRemittanceCompletedEvent(RemittanceCompletedEvent remittanceCompletedEvent){
        rentalService.completeRemittanceProcessing(remittanceCompletedEvent);
    }
}
