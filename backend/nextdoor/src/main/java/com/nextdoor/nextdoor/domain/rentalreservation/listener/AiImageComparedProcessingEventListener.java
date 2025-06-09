package com.nextdoor.nextdoor.domain.rentalreservation.listener;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationProcess;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationStatus;
import com.nextdoor.nextdoor.domain.rentalreservation.event.out.DepositProcessingRequestEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.event.out.RentalCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.message.RentalStatusMessage;
import com.nextdoor.nextdoor.domain.rentalreservation.port.MemberUuidQueryPort;
import com.nextdoor.nextdoor.domain.rentalreservation.port.RentalDetailQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Component
@RequiredArgsConstructor
public class AiImageComparedProcessingEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final MemberUuidQueryPort memberUuidQueryPort;
    private final RentalDetailQueryPort rentalDetailQueryPort;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRentalCompletedEvent(RentalCompletedEvent event) {
        String renterUuid = memberUuidQueryPort.getMemberUuidByRentalIdAndRole(
                 event.getRentalId(),
                "RENTER"
        );

        RentalStatusMessage.RentalDetailResult rentalDetailResult = rentalDetailQueryPort.getRentalDetailByRentalIdAndRole(
                event.getRentalId()
        );

        messagingTemplate.convertAndSend(
                "/topic/rental-reservation/" + renterUuid + "/status",
                RentalStatusMessage.builder()
                        .process(RentalReservationProcess.RENTAL_COMPLETED.name())
                        .detailStatus(RentalReservationStatus.RENTAL_COMPLETED.name())
                        .rentalDetail(rentalDetailResult)
                        .build()
        );

        messagingTemplate.convertAndSend(
                "/topic/rental-reservation/" + event.getRentalId() + "/status",
                RentalStatusMessage.builder()
                        .process(RentalReservationProcess.RENTAL_COMPLETED.name())
                        .detailStatus(RentalReservationStatus.RENTAL_COMPLETED.name())
                        .rentalDetail(rentalDetailResult)
                        .build()
        );
    }

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDepositProcessingRequestEvent(DepositProcessingRequestEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/rental-reservation/" + event.getRentalId() + "/status",
                RentalStatusMessage.builder()
                        .process(RentalReservationProcess.RETURNED.name())
                        .detailStatus(RentalReservationStatus.BEFORE_AND_AFTER_COMPARED.name())
                        .build()
        );
    }
}
