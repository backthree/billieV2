package com.nextdoor.nextdoor.domain.rentalreservation.application.service;

import com.nextdoor.nextdoor.domain.fintech.event.DepositCompletedEvent;
import com.nextdoor.nextdoor.domain.fintech.event.RemittanceCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.*;
import com.nextdoor.nextdoor.domain.rentalreservation.application.port.*;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.*;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.exception.NoSuchRentalException;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.exception.NoSuchReservationException;
import com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.message.RentalStatusMessage;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.repository.RentalReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalSettlementServiceImpl implements RentalSettlementService {

    private final RentalReservationRepository rentalReservationRepository;
    private final RentalQueryPort rentalQueryPort;
    private final MemberUuidQueryPort memberUuidQueryPort;
    private final RentalDetailQueryPort rentalDetailQueryPort;
    private final RentalScheduleService rentalScheduleService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public RequestRemittanceResult requestRemittance(RequestRemittanceCommand command) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(command.getRentalId())
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        rentalReservation.processRemittanceRequest();

        return rentalQueryPort.findRemittanceRequestViewData(command.getRentalId())
                .orElseThrow(() -> new NoSuchReservationException("예약 정보가 존재하지 않습니다."));
    }

    @Override
    @Transactional(readOnly = true)
    public RequestRemittanceResult getRemittanceData(Long rentalId) {
        return rentalQueryPort.findRemittanceRequestViewData(rentalId)
                .orElseThrow(() -> new NoSuchReservationException("예약 정보가 존재하지 않습니다."));
    }

    @Override
    @Transactional
    public void completeRemittanceProcessing(RemittanceCompletedEvent remittanceCompletedEvent) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(remittanceCompletedEvent.getRentalId())
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        rentalReservation.processRemittanceCompletion();

        //테스트 용도
        rentalScheduleService.scheduleRentalEnd(rentalReservation.getId());

        String ownerUuid = memberUuidQueryPort.getMemberUuidByRentalIdAndRole(
                rentalReservation.getId(),
                "OWNER"
        );

        RentalStatusMessage.RentalDetailResult rentalDetailResult = rentalDetailQueryPort.getRentalDetailByRentalIdAndRole(
                rentalReservation.getId()
        );

        messagingTemplate.convertAndSend("/topic/rental-reservation/" + ownerUuid + "/status",
                RentalStatusMessage.builder()
                        .rentalId(rentalReservation.getId())
                        .process(RentalReservationProcess.RENTAL_IN_ACTIVE.name())
                        .detailStatus(RentalReservationStatus.REMITTANCE_COMPLETED.name())
                        .rentalDetail(rentalDetailResult)
                        .build()
        );

        messagingTemplate.convertAndSend(
                "/topic/rental-reservation/" + rentalReservation.getId() + "/status",
                RentalStatusMessage.builder()
                        .process(RentalReservationProcess.RENTAL_IN_ACTIVE.name())
                        .detailStatus(RentalReservationStatus.REMITTANCE_COMPLETED.name())
                        .rentalDetail(rentalDetailResult)
                        .build()
        );
    }

    @Override
    @Transactional
    public void completeDepositProcessing(DepositCompletedEvent depositCompletedEvent) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(depositCompletedEvent.getRentalId())
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        rentalReservation.processDepositCompletion();
    }

    @Override
    @Transactional
    public void updateRentalDepositId(Long rentalId, Long depositId) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(rentalId)
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        rentalReservation.updateDepositId(depositId);
    }

    @Override
    @Transactional
    public UpdateAccountResult updateAccount(UpdateAccountCommand command) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(command.getRentalId())
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        rentalReservation.processUpdateAccountInfo(command.getAccountNo(), command.getBankCode());
        rentalReservation.updateFinalAmount(new Money(command.getFinalAmount()));

        String renterUuid = memberUuidQueryPort.getMemberUuidByRentalIdAndRole(
                rentalReservation.getId(),
                "RENTER"
        );

        RentalStatusMessage.RentalDetailResult rentalDetailResult = rentalDetailQueryPort.getRentalDetailByRentalIdAndRole(
                rentalReservation.getId()
        );

        messagingTemplate.convertAndSend(
                "/topic/rental-reservation/" + renterUuid + "/status",
                RentalStatusMessage.builder()
                        .rentalId(rentalReservation.getId())
                        .process(RentalReservationProcess.BEFORE_RENTAL.name())
                        .detailStatus(RentalReservationStatus.REMITTANCE_REQUESTED.name())
                        .rentalDetail(rentalDetailResult)
                        .build()
        );

        messagingTemplate.convertAndSend(
                "/topic/rental-reservation/" + rentalReservation.getId() + "/status",
                RentalStatusMessage.builder()
                        .process(RentalReservationProcess.BEFORE_RENTAL.name())
                        .detailStatus(RentalReservationStatus.REMITTANCE_REQUESTED.name())
                        .rentalDetail(rentalDetailResult)
                        .build()
        );

        return UpdateAccountResult.builder()
                .rentalId(rentalReservation.getId())
                .accountNo(rentalReservation.getAccountInfo().getAccountNo())
                .bankCode(rentalReservation.getAccountInfo().getBankCode())
                .finalAmount(rentalReservation.getFinalAmount().getAmount())
                .build();
    }
}