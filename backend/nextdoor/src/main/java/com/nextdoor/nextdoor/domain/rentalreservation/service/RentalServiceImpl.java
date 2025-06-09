package com.nextdoor.nextdoor.domain.rentalreservation.service;

import com.nextdoor.nextdoor.domain.fintech.event.DepositCompletedEvent;
import com.nextdoor.nextdoor.domain.fintech.event.RemittanceCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.*;
import com.nextdoor.nextdoor.domain.rentalreservation.domainservice.RentalDomainService;
import com.nextdoor.nextdoor.domain.rentalreservation.domainservice.RentalImageDomainService;
import com.nextdoor.nextdoor.domain.rentalreservation.event.RentalReservationConfirmedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.event.out.DepositProcessingRequestEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.event.out.RentalCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.InvalidRenterIdException;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.NoSuchRentalException;
import com.nextdoor.nextdoor.domain.rentalreservation.message.RentalStatusMessage;
import com.nextdoor.nextdoor.domain.rentalreservation.port.*;
import com.nextdoor.nextdoor.domain.rentalreservation.repository.AiImageComparisonPairRepository;
import com.nextdoor.nextdoor.domain.rentalreservation.repository.RentalReservationRepository;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.*;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.NoSuchReservationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {

    private final RentalReservationRepository rentalReservationRepository;
    private final AiImageComparisonPairRepository aiImageComparisonPairRepository;
    private final S3ImageUploadPort s3ImageUploadService;
    private final ReservationQueryPort reservationQueryPort;
    private final RentalQueryPort rentalQueryPort;
    private final MemberUuidQueryPort memberUuidQueryPort;
    private final RentalDetailQueryPort rentalDetailQueryPort;
    private final RentalScheduleService rentalScheduleService;
    private final RentalDomainService rentalDomainService;
    private final RentalImageDomainService rentalImageDomainService;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Page<SearchRentalResult> searchRentals(SearchRentalCommand command) {
        return rentalQueryPort.searchRentals(command);
    }

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
    public UploadImageResult registerBeforePhoto(UploadImageCommand command) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(command.getRentalId())
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        ReservationDto reservation = reservationQueryPort.getReservationByRentalId(rentalReservation.getId())
                .orElseThrow(() -> new NoSuchReservationException("예약 정보가 존재하지 않습니다."));

        if (!reservation.getRenterId().equals(command.getUserId())) {
            throw new InvalidRenterIdException("요청한 Renter ID가 실제 Renter ID와 일치하지 않습니다.");
        }

        String path = rentalImageDomainService.createImagePath(
                String.valueOf(rentalReservation.getId()),
                AiImageType.BEFORE
        );

        List<String> imageUrls = new ArrayList<>();
        for(MultipartFile image : command.getImages()){
            S3UploadResult imageUploadResult = s3ImageUploadService.upload(image, path);
            imageUrls.add(imageUploadResult.getUrl());

            rentalImageDomainService.processRentalImage(
                    rentalReservation,
                    imageUploadResult.getUrl(),
                    image.getContentType(),
                    AiImageType.BEFORE
            );
        }

        LocalDateTime uploadedAt = LocalDateTime.now();
        return UploadImageResult.builder()
                .rentalId(rentalReservation.getId())
                .imageUrls(imageUrls)
                .uploadedAt(uploadedAt)
                .build();
    }

    @Override
    @Transactional
    public void completeRemittanceProcessing(RemittanceCompletedEvent remittanceCompletedEvent){
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
    public UploadImageResult registerAfterPhoto(UploadImageCommand command) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(command.getRentalId())
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        ReservationDto reservation = reservationQueryPort.getReservationByRentalId(rentalReservation.getId())
                .orElseThrow(() -> new NoSuchReservationException("예약 정보가 존재하지 않습니다."));

        if (!reservation.getOwnerId().equals(command.getUserId())) {
            throw new InvalidRenterIdException("요청한 Owner ID가 실제 Owner ID와 일치하지 않습니다.");
        }

        String path = rentalImageDomainService.createImagePath(
                String.valueOf(rentalReservation.getId()),
                AiImageType.AFTER
        );

        List<String> imageUrls = new ArrayList<>();
        for(MultipartFile image : command.getImages()){
            S3UploadResult imageUploadResult = s3ImageUploadService.upload(image, path);
            imageUrls.add(imageUploadResult.getUrl());

            rentalImageDomainService.processRentalImage(
                    rentalReservation,
                    imageUploadResult.getUrl(),
                    image.getContentType(),
                    AiImageType.AFTER
            );
        }

        LocalDateTime uploadedAt = LocalDateTime.now();
        return UploadImageResult.builder()
                .rentalId(rentalReservation.getId())
                .imageUrls(imageUrls)
                .uploadedAt(uploadedAt)
                .build();
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
    public AiComparisonResult getAiAnalysis(Long rentalId) {
        rentalReservationRepository.findById(rentalId)
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        return rentalReservationRepository.findRentalWithImagesByRentalId(rentalId).orElse(null);
    }

    @Override
    @Transactional
    public void updateDamageAnalysis(Long rentalId, String damageAnalysis) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(rentalId)
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        rentalReservation.updateDamageAnalysis(damageAnalysis);
    }

    @Override
    @Transactional
    public void updateComparedAnalysis(Long rentalId, String comparedAnalysis) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(rentalId)
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        rentalReservation.updateComparedAnalysis(comparedAnalysis);

        ReservationDto reservationDto = reservationQueryPort.getReservationByRentalId(rentalReservation.getId())
                .orElseThrow(() -> new NoSuchReservationException("예약 정보가 존재하지 않습니다."));

        rentalDomainService.processAfterImageRegistration(rentalReservation, reservationDto.getDeposit());

        if(rentalReservation.getRentalReservationStatus().equals(RentalReservationStatus.BEFORE_AND_AFTER_COMPARED)){
            eventPublisher.publishEvent(DepositProcessingRequestEvent.builder()
                    .rentalId(rentalReservation.getId())
                    .build());
        } else if(rentalReservation.getRentalReservationStatus().equals(RentalReservationStatus.RENTAL_COMPLETED)){
            rentalReservation.updateDealCount();
            eventPublisher.publishEvent(RentalCompletedEvent.builder()
                    .rentalId(rentalReservation.getId())
                    .build());
        }
    }

    @Override
    @Transactional
    public UpdateAccountResult updateAccount(UpdateAccountCommand command) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(command.getRentalId())
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        rentalReservation.processUpdateAccountInfo(command.getAccountNo(), command.getBankCode());
        rentalReservation.updateFinalAmount(command.getFinalAmount());

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
                .accountNo(rentalReservation.getAccountNo())
                .bankCode(rentalReservation.getBankCode())
                .finalAmount(rentalReservation.getFinalAmount())
                .build();
    }

    @Override
    public ManagedRentalCountResult countManagedRentals(Long ownerId) {
        return rentalQueryPort.countManagedRentals(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchRentalResult getRentalById(Long rentalId) {
        return rentalQueryPort.findRentalById(rentalId)
                .orElseThrow(() -> new NoSuchRentalException("ID가 " + rentalId + "인 대여 정보가 존재하지 않습니다."));
    }

    @Override
    @Transactional
    public DeleteRentalResult deleteRental(DeleteRentalCommand command) {
        try {
            RentalReservation rental = rentalReservationRepository.findById(command.getRentalId())
                    .orElseThrow(() -> new NoSuchRentalException("ID가 " + command.getRentalId() + "인 대여 정보가 존재하지 않습니다."));

            rentalReservationRepository.delete(rental);

            return DeleteRentalResult.builder()
                    .rentalId(command.getRentalId())
                    .success(true)
                    .message("대여 정보가 성공적으로 삭제되었습니다.")
                    .build();
        } catch (NoSuchRentalException e) {
            return DeleteRentalResult.builder()
                    .rentalId(command.getRentalId())
                    .success(false)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            return DeleteRentalResult.builder()
                    .rentalId(command.getRentalId())
                    .success(false)
                    .message("대여 정보 삭제 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public void createAiImageComparisonPair(Long rentalId, Long beforeImageId, Long afterImageId, String pairComparisonResult) {
        aiImageComparisonPairRepository.save(new AiImageComparisonPair(rentalId, beforeImageId, afterImageId, pairComparisonResult));
    }

    @Override
    @Transactional
    public void deleteAiImageComparisonPairByRentalId(Long rentalId) {
        aiImageComparisonPairRepository.deleteByRentalId(rentalId);
    }

    @Override
    @Transactional
    public void createFromRentalReservation(RentalReservationConfirmedEvent event) {
        // Implementation for handling RentalReservationConfirmedEvent
        // This method replaces the old createFromReservation method
        RentalReservation rentalReservation = rentalReservationRepository.findById(event.getRentalReservationId())
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        // Add any necessary logic here to handle the confirmed event
        // For now, just a placeholder implementation
    }
}
