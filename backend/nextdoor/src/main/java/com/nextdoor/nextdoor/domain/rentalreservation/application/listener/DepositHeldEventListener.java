package com.nextdoor.nextdoor.domain.rentalreservation.application.listener;

import com.nextdoor.nextdoor.domain.fintech.event.DepositHeldEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.application.service.RentalSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DepositHeldEventListener {

    private final RentalSettlementService rentalSettlementService;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleDepositHeldEvent(DepositHeldEvent event) {
        rentalSettlementService.updateRentalDepositId(event.getRentalId(), event.getDepositId());
    }
}
