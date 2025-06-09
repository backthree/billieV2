package com.nextdoor.nextdoor.domain.rentalreservation.service;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationProcess;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationStatus;
import com.nextdoor.nextdoor.domain.rentalreservation.message.RentalStatusMessage;
import com.nextdoor.nextdoor.domain.rentalreservation.repository.RentalReservationRepository;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.NoSuchRentalException;
import com.nextdoor.nextdoor.domain.rentalreservation.port.RentalDetailQueryPort;
import com.nextdoor.nextdoor.domain.rentalreservation.port.MemberUuidQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalEndService {

    private final RentalReservationRepository rentalRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MemberUuidQueryPort memberUuidQueryPort;
    private final RentalDetailQueryPort rentalDetailQueryPort;

    @Transactional
    public void rentalEnd(Long rentalId) {
        System.out.println("[DEBUG_LOG] RentalEndService.rentalEnd called for rental ID: " + rentalId);
        try {
            RentalReservation rental = rentalRepository.findById(rentalId)
                    .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

            rental.processRentalPeriodEnd();

            String renterUuid = memberUuidQueryPort.getMemberUuidByRentalIdAndRole(
                    rental.getId(),
                    "RENTER"
            );

            RentalStatusMessage.RentalDetailResult rentalDetailResult = rentalDetailQueryPort.getRentalDetailByRentalIdAndRole(
                    rental.getId()
            );

            messagingTemplate.convertAndSend("/topic/rental-reservation/" + renterUuid + "/status"
                    , RentalStatusMessage.builder()
                            .rentalId(rental.getId())
                            .process(RentalReservationProcess.RETURNED.name())
                            .detailStatus(RentalReservationStatus.RENTAL_PERIOD_ENDED.name())
                            .rentalDetail(rentalDetailResult)
                            .build()
            );

            messagingTemplate.convertAndSend(
                    "/topic/rental-reservation/" + rental.getId() + "/status",
                    RentalStatusMessage.builder()
                            .process(RentalReservationProcess.RETURNED.name())
                            .detailStatus(RentalReservationStatus.RENTAL_PERIOD_ENDED.name())
                            .rentalDetail(rentalDetailResult)
                            .build()
            );

            System.out.println("[DEBUG_LOG] completeRentalEndProcessing completed successfully for rental ID: " + rentalId);
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Error in completeRentalEndProcessing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
