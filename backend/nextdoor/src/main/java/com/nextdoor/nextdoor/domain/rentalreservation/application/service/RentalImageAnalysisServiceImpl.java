package com.nextdoor.nextdoor.domain.rentalreservation.application.service;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.*;
import com.nextdoor.nextdoor.domain.rentalreservation.application.port.*;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.service.RentalDomainService;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.service.RentalImageDomainService;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.*;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.event.out.DepositProcessingRequestEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.event.out.RentalCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.exception.InvalidRenterIdException;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.exception.NoSuchRentalException;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.exception.NoSuchReservationException;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.repository.AiImageComparisonPairRepository;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.repository.RentalReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalImageAnalysisServiceImpl implements RentalImageAnalysisService {

    private final RentalReservationRepository rentalReservationRepository;
    private final AiImageComparisonPairRepository aiImageComparisonPairRepository;
    private final S3ImageUploadPort s3ImageUploadService;
    private final ReservationQueryPort reservationQueryPort;
    private final RentalDomainService rentalDomainService;
    private final RentalImageDomainService rentalImageDomainService;
    private final ApplicationEventPublisher eventPublisher;

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

        List<String> imageUrls = new ArrayList<>();
        for(MultipartFile image : command.getImages()){
            S3UploadResult imageUploadResult = s3ImageUploadService.upload(
                    image,
                    rentalReservation.getId(),
                    AiImageType.BEFORE.toString()
            );

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
    public UploadImageResult registerAfterPhoto(UploadImageCommand command) {
        RentalReservation rentalReservation = rentalReservationRepository.findById(command.getRentalId())
                .orElseThrow(() -> new NoSuchRentalException("대여 정보가 존재하지 않습니다."));

        ReservationDto reservation = reservationQueryPort.getReservationByRentalId(rentalReservation.getId())
                .orElseThrow(() -> new NoSuchReservationException("예약 정보가 존재하지 않습니다."));

        if (!reservation.getOwnerId().equals(command.getUserId())) {
            throw new InvalidRenterIdException("요청한 Owner ID가 실제 Owner ID와 일치하지 않습니다.");
        }

        List<String> imageUrls = new ArrayList<>();
        for(MultipartFile image : command.getImages()){
            S3UploadResult imageUploadResult = s3ImageUploadService.upload(
                    image,
                    rentalReservation.getId(),
                    AiImageType.AFTER.toString()
            );

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
            eventPublisher.publishEvent(RentalCompletedEvent.builder()
                    .rentalId(rentalReservation.getId())
                    .build());
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
}