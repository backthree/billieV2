package com.nextdoor.nextdoor.domain.rentalreservation.service;

import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request.ReservationSaveRequestDto;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request.ReservationStatusUpdateRequestDto;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request.ReservationUpdateRequestDto;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response.ReservationResponseDto;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationProcess;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationStatus;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.AlreadyConfirmedException;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.IllegalStatusException;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.NoSuchReservationException;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.UnauthorizedException;
import com.nextdoor.nextdoor.domain.rentalreservation.message.RentalStatusMessage;
import com.nextdoor.nextdoor.domain.rentalreservation.message.RequestRemittanceStatusMessage;
import com.nextdoor.nextdoor.domain.rentalreservation.port.MemberUuidQueryPort;
import com.nextdoor.nextdoor.domain.rentalreservation.port.RentalDetailQueryPort;
import com.nextdoor.nextdoor.domain.rentalreservation.port.RentalReservationPostQueryPort;
import com.nextdoor.nextdoor.domain.rentalreservation.port.ReservationMemberQueryPort;
import com.nextdoor.nextdoor.domain.rentalreservation.repository.RentalReservationRepository;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.PostDto;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.ReservationMemberQueryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class ReservationService {

    private final MemberUuidQueryPort memberUuidQueryPort;
    private final RentalDetailQueryPort rentalDetailQueryPort;
    private final RentalReservationRepository rentalReservationRepository;
    private final RentalReservationPostQueryPort rentalReservationPostQueryPort;
    private final ReservationMemberQueryPort reservationMemberQueryPort;
    private final SimpMessagingTemplate messagingTemplate;

    public ReservationResponseDto createReservation(Long loginUserId, ReservationSaveRequestDto reservationSaveRequestDto) {
        PostDto post = rentalReservationPostQueryPort.findById(reservationSaveRequestDto.getPostId()).orElseThrow();

        RentalReservation rentalReservation = rentalReservationRepository.save(RentalReservation.builder()
            .startDate(reservationSaveRequestDto.getStartDate())
            .endDate(reservationSaveRequestDto.getEndDate())
            .rentalFee(post.getRentalFee())
            .deposit(post.getDeposit())
            .rentalReservationStatus(RentalReservationStatus.PENDING)
            .ownerId(post.getAuthorId())
            .renterId(loginUserId)
            .postId(post.getPostId())
            .build());

        ReservationMemberQueryDto member = reservationMemberQueryPort.findById(loginUserId).orElseThrow();
        ReservationResponseDto response = ReservationResponseDto.from(rentalReservation, post, member);
        messagingTemplate.convertAndSend("/topic/rental-reservation/" + post.getAuthorUuid() + "/status", response);
        return response;
    }

    public ReservationResponseDto updateReservation(Long loginUserId, Long reservationId, ReservationUpdateRequestDto reservationUpdateRequestDto) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(reservationId).orElseThrow(NoSuchReservationException::new);
        validateOwner(loginUserId, rentalReservation);

        rentalReservation.updateStartDate(reservationUpdateRequestDto.getStartDate());
        rentalReservation.updateEndDate(reservationUpdateRequestDto.getEndDate());
        rentalReservation.updateRentalFee(reservationUpdateRequestDto.getRentalFee());
        rentalReservation.updateDeposit(reservationUpdateRequestDto.getDeposit());

        return ReservationResponseDto.from(
                rentalReservation,
                rentalReservationPostQueryPort.findById(rentalReservation.getPostId()).orElseThrow(),
                reservationMemberQueryPort.findById(loginUserId).orElseThrow());
    }

    public void confirmReservation(Long loginUserId, Long reservationId, ReservationStatusUpdateRequestDto reservationStatusUpdateRequestDto) {
        if (reservationStatusUpdateRequestDto.getStatus() != RentalReservationStatus.CONFIRMED) {
            throw new IllegalStatusException("잘못된 status입니다.");
        }
        RentalReservation rentalReservation = rentalReservationRepository.findById(reservationId).orElseThrow(NoSuchReservationException::new);

        updateReservationStatus(loginUserId, rentalReservation, reservationStatusUpdateRequestDto);

        String ownerUuid = memberUuidQueryPort.getMemberUuidByRentalIdAndRole(
                rentalReservation.getId(),
                "OWNER"
        );

        RentalStatusMessage.RentalDetailResult rentalDetailResult = rentalDetailQueryPort.getRentalDetailByRentalIdAndRole(
                rentalReservation.getId()
        );

        messagingTemplate.convertAndSend(
                "/topic/rental-reservation/" + ownerUuid + "/status",
                RequestRemittanceStatusMessage.builder()
                        .rentalId(rentalReservation.getId())
                        .process(RentalReservationProcess.BEFORE_RENTAL.name())
                        .detailStatus(RentalReservationStatus.CONFIRMED.name())
                        .rentalDetail(rentalDetailResult)
                        .build()
        );

        messagingTemplate.convertAndSend(
                "/topic/rental-reservation/" + rentalReservation.getId() + "/status",
                RentalStatusMessage.builder()
                        .process(RentalReservationProcess.BEFORE_RENTAL.name())
                        .detailStatus(RentalReservationStatus.CONFIRMED.name())
                        .rentalDetail(rentalDetailResult)
                        .build()
        );
    }

    public ReservationResponseDto updateReservationStatus(Long loginUserId, RentalReservation rentalReservation, ReservationStatusUpdateRequestDto reservationStatusUpdateRequestDto) {

        validateOwner(loginUserId, rentalReservation);
        validateNotConfirmed(rentalReservation);
        rentalReservation.updateStatus(reservationStatusUpdateRequestDto.getStatus());
        return ReservationResponseDto.from(
                rentalReservation,
                rentalReservationPostQueryPort.findById(rentalReservation.getPostId()).orElseThrow(),
                reservationMemberQueryPort.findById(loginUserId).orElseThrow());
    }

    public void deleteReservation(Long loginUserId, Long reservationId) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(reservationId).orElseThrow(NoSuchReservationException::new);
        validateOwnerOrRenter(loginUserId, rentalReservation);
        validateNotConfirmed(rentalReservation);
        rentalReservationRepository.delete(rentalReservation);
    }

    private void validateNotConfirmed(RentalReservation rentalReservation) {
        if (rentalReservation.getRentalReservationStatus() == RentalReservationStatus.CONFIRMED) {
            throw new AlreadyConfirmedException("이미 확정된 예약입니다.");
        }
    }

    private void validateOwner(Long loginUserId, RentalReservation rentalReservation) {
        if (!rentalReservation.getOwnerId().equals(loginUserId)) {
            throw new UnauthorizedException("권한이 없습니다.");
        }
    }

    private void validateOwnerOrRenter(Long loginUserId, RentalReservation rentalReservation) {
        if (!rentalReservation.getOwnerId().equals(loginUserId) && !rentalReservation.getRenterId().equals(loginUserId)) {
            throw new UnauthorizedException("권한이 없습니다.");
        }
    }
}
