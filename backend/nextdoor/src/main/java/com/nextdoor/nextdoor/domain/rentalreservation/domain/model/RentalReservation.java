package com.nextdoor.nextdoor.domain.rentalreservation.domain.model;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.exception.InvalidRentalStatusException;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.util.ValidationUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rental_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RentalReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_reservation_id")
    private Long id;

    @OneToMany(mappedBy = "rental", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiImage> aiImages = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "rental_status", nullable = false, length = 50)
    private RentalReservationStatus rentalReservationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "rental_process", nullable = false, length = 50)
    private RentalReservationProcess rentalReservationProcess;

    @Lob
    @Column(name = "damage_analysis", columnDefinition = "TEXT")
    private String damageAnalysis;

    @Lob
    @Column(name = "compared_analysis", columnDefinition = "TEXT")
    private String comparedAnalysis;

    @Embedded
    private AccountInfo accountInfo;

    @Column(name = "deposit_id")
    private Long depositId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "final_amount"))
    })
    private Money finalAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "start_date")
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date")
    private LocalDate endDate;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "rentalFee"))
    })
    private Money rentalFee;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "deposit"))
    })
    private Money deposit;

    @NotNull
    @Column(name = "owner_id")
    private Long ownerId;

    @NotNull
    @Column(name = "renter_id")
    private Long renterId;

    @NotNull
    @Column(name = "post_id")
    private Long postId;

    public static RentalReservation create(
            List<AiImage> initialAiImages,
            RentalReservationStatus rentalReservationStatus,
            String damageAnalysis,
            String accountNo,
            String bankCode,
            Long depositId,
            LocalDate startDate,
            LocalDate endDate,
            Money rentalFee,
            Money deposit,
            Long ownerId,
            Long renterId,
            Long postId) {

        AiImages images = new AiImages(initialAiImages);

        return RentalReservation.builder()
                .aiImages(images.getImages())
                .rentalReservationStatus(rentalReservationStatus)
                .rentalReservationProcess(getRentalProcessForStatus(rentalReservationStatus))
                .damageAnalysis(damageAnalysis)
                .createdAt(LocalDateTime.now())
                .accountInfo(new AccountInfo(accountNo, bankCode))
                .depositId(depositId)
                .startDate(startDate)
                .endDate(endDate)
                .rentalFee(rentalFee)
                .deposit(deposit)
                .ownerId(ownerId)
                .renterId(renterId)
                .postId(postId)
                .build();
    }

    @Builder(access = AccessLevel.PRIVATE)
    private RentalReservation(List<AiImage> aiImages, RentalReservationStatus rentalReservationStatus, RentalReservationProcess rentalReservationProcess,
                              String damageAnalysis, LocalDateTime createdAt, AccountInfo accountInfo, Long depositId,
                              LocalDate startDate, LocalDate endDate, Money rentalFee, Money deposit,
                              Long ownerId, Long renterId, Long postId) {
        this.aiImages = aiImages;
        this.rentalReservationStatus = rentalReservationStatus;
        this.rentalReservationProcess = rentalReservationProcess;
        this.damageAnalysis = damageAnalysis;
        this.createdAt = createdAt;
        this.accountInfo = accountInfo;
        this.depositId = depositId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rentalFee = rentalFee;
        this.deposit = deposit;
        this.ownerId = ownerId;
        this.renterId = renterId;
        this.postId = postId;
    }

    public void processRemittanceRequest() {
        updateStatus(RentalReservationStatus.REMITTANCE_REQUESTED);
    }

    public void processRemittanceCompletion() {
        validateRemittanceCompletionStatus();
        updateStatus(RentalReservationStatus.REMITTANCE_COMPLETED);
    }

    private void validateRemittanceCompletionStatus() {
        if(rentalReservationStatus != RentalReservationStatus.BEFORE_PHOTO_ANALYZED){
            throw new InvalidRentalStatusException("결제 완료 처리가 불가능한 대여 상태입니다");
        }
    }

    public void processRentalPeriodEnd() {
        validateRentalPeriodEndStatus();
        updateStatus(RentalReservationStatus.RENTAL_PERIOD_ENDED);
    }

    private void validateRentalPeriodEndStatus() {
        if(rentalReservationStatus != RentalReservationStatus.REMITTANCE_COMPLETED){
            throw new InvalidRentalStatusException("대여 기간 종료가 불가능한 대여 상태입니다");
        }
    }

    public void updateDamageAnalysis(String damageAnalysis) {
        this.damageAnalysis = damageAnalysis;
        updateStatus(RentalReservationStatus.BEFORE_PHOTO_ANALYZED);
    }

    public void updateComparedAnalysis(String comparedAnalysis) {
        this.comparedAnalysis = comparedAnalysis;
    }

    public void processUpdateAccountInfo(String accountNo, String bankCode) {
        ValidationUtils.validateNotBlank(accountNo, "accountNo");
        ValidationUtils.validateNotBlank(bankCode, "bankCode");
        this.accountInfo = new AccountInfo(accountNo, bankCode);

        updateStatus(RentalReservationStatus.REMITTANCE_REQUESTED);
    }

    public void processDepositCompletion(){
        validateDepositCompletionStatus();
        updateStatus(RentalReservationStatus.RENTAL_COMPLETED);
    }

    private void validateDepositCompletionStatus() {
        if (rentalReservationStatus != RentalReservationStatus.BEFORE_AND_AFTER_COMPARED) {
            throw new InvalidRentalStatusException(this.rentalReservationStatus.name() + ": 보증금을 처리가 불가능한 대여 상태입니다");
        }
    }

    public void updateDepositId(Long depositId) {
        this.depositId = depositId;
    }

    public void updateFinalAmount(Money amount) {
        this.finalAmount = amount;
    }

    public void saveAiImage(AiImageType imageType, String imageUrl, String mimeType) {
        AiImage newImage = AiImage.create(this, imageType, imageUrl, mimeType);
        AiImages imagesWrapper = new AiImages(this.aiImages);
        imagesWrapper.addImage(newImage);
        this.aiImages.add(newImage);
    }

    public void updateStatus(RentalReservationStatus rentalReservationStatus){
        this.rentalReservationStatus = rentalReservationStatus;
        this.rentalReservationProcess = getRentalProcessForStatus(rentalReservationStatus);
    }

    public void updateStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void updateEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void updateRentalFee(Money rentalFee) {
        this.rentalFee = rentalFee;
    }

    public void updateDeposit(Money deposit) {
        this.deposit = deposit;
    }

    private static RentalReservationProcess getRentalProcessForStatus(RentalReservationStatus status) {
        if (status == null) {
            return RentalReservationProcess.BEFORE_RENTAL;
        }

        switch (status) {
            case PENDING:
            case CONFIRMED:
            case BEFORE_PHOTO_ANALYZED:
            case REMITTANCE_REQUESTED:
            case CANCELLED:
                return RentalReservationProcess.BEFORE_RENTAL;
            case REMITTANCE_COMPLETED:
                return RentalReservationProcess.RENTAL_IN_ACTIVE;
            case RENTAL_PERIOD_ENDED:
            case BEFORE_AND_AFTER_COMPARED:
            case DEPOSIT_REQUESTED:
                return RentalReservationProcess.RETURNED;
            case RENTAL_COMPLETED:
                return RentalReservationProcess.RENTAL_COMPLETED;
            default:
                return RentalReservationProcess.BEFORE_RENTAL;
        }
    }
}
