package com.nextdoor.nextdoor.domain.rentalreservation.domain;

import com.nextdoor.nextdoor.domain.rentalreservation.exception.InvalidAmountException;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.InvalidRentalStatusException;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.RentalImageUploadException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @Column(name = "deal_count")
    private Integer dealCount;

    @Column(name = "account_no", nullable = false, length = 30)
    private String accountNo;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Column(name = "deposit_id")
    private Long depositId;

    @Column(name = "final_amount")
    private BigDecimal finalAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "start_date")
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date")
    private LocalDate endDate;

    @NotNull
    @Column(name = "rental_fee")
    private BigDecimal rentalFee;

    @NotNull
    @Column(name = "deposit")
    private BigDecimal deposit;

    @NotNull
    @Column(name = "owner_id")
    private Long ownerId;

    @NotNull
    @Column(name = "renter_id")
    private Long renterId;

    @NotNull
    @Column(name = "post_id")
    private Long postId;

    @Builder
    public RentalReservation(List<AiImage> aiImages, RentalReservationStatus rentalReservationStatus, RentalReservationProcess rentalReservationProcess,
                             String damageAnalysis, LocalDateTime createdAt, String accountNo, String bankCode, Long depositId,
                             LocalDate startDate, LocalDate endDate, BigDecimal rentalFee, BigDecimal deposit,
                             Long ownerId, Long renterId, Long postId) {
        this.aiImages = aiImages;
        this.rentalReservationStatus = rentalReservationStatus;
        this.rentalReservationProcess = rentalReservationProcess != null ? rentalReservationProcess : getRentalProcessForStatus(rentalReservationStatus);
        this.damageAnalysis = damageAnalysis;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.dealCount = 0;
        this.accountNo = accountNo != null ? accountNo : "";
        this.bankCode = bankCode != null ? bankCode : "";
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
        if(rentalReservationStatus != RentalReservationStatus.BEFORE_PHOTO_ANALYZED){
            throw new InvalidRentalStatusException("결제 완료 처리가 불가능한 대여 상태입니다");
        }

        updateStatus(RentalReservationStatus.REMITTANCE_COMPLETED);
    }

    public void processRentalPeriodEnd() {
        if(rentalReservationStatus != RentalReservationStatus.REMITTANCE_COMPLETED){
            throw new InvalidRentalStatusException("대여 기간 종료가 불가능한 대여 상태입니다");
        }

        updateStatus(RentalReservationStatus.RENTAL_PERIOD_ENDED);
    }

    public void updateDamageAnalysis(String damageAnalysis) {
        this.damageAnalysis = damageAnalysis;
        updateStatus(RentalReservationStatus.BEFORE_PHOTO_ANALYZED);
    }

    public void updateComparedAnalysis(String comparedAnalysis) {
        this.comparedAnalysis = comparedAnalysis;
    }

    public void processUpdateAccountInfo(String accountNo, String bankCode) {
        validateNotBlank(accountNo, "accountNo");
        validateNotBlank(bankCode, "bankCode");
        this.accountNo = accountNo;
        this.bankCode = bankCode;

        updateStatus(RentalReservationStatus.REMITTANCE_REQUESTED);
    }

    public void processDepositCompletion(){
        if (rentalReservationStatus != RentalReservationStatus.BEFORE_AND_AFTER_COMPARED) {
            throw new InvalidRentalStatusException(this.rentalReservationStatus.name() + ": 보증금을 처리가 불가능한 대여 상태입니다");
        }

        updateDealCount();
        updateStatus(RentalReservationStatus.RENTAL_COMPLETED);
    }

    public void updateDealCount() {
        this.dealCount = dealCount + 1;
    }

    public void updateDepositId(Long depositId) {
        this.depositId = depositId;
    }

    public void updateFinalAmount(BigDecimal amount) {
        validateAmount(amount);
        this.finalAmount = amount;
    }

    public void validateRemittancePendingState() {
        if (this.rentalReservationStatus != RentalReservationStatus.REMITTANCE_REQUESTED) {
            throw new InvalidRentalStatusException("송금 대기 상태가 아닙니다.");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("송금 금액은 필수입니다.");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("송금 금액은 0보다 커야 합니다.");
        }
    }

    public void saveAiImage(AiImageType imageType, String imageUrl, String mimeType) {
        validateNotBlank(imageUrl, "imageUrl");
        validateNotBlank(mimeType, "mimeType");
        validateQuantityLimit();
        validateNoDuplicate(imageUrl);

        AiImage aiImage = AiImage.builder()
                .rental(this)
                .type(imageType)
                .imageUrl(imageUrl)
                .mimeType(mimeType)
                .build();
        aiImages.add(aiImage);
    }

    public void updateStatus(RentalReservationStatus rentalReservationStatus){
        this.rentalReservationStatus = rentalReservationStatus;
        this.rentalReservationProcess = getRentalProcessForStatus(rentalReservationStatus);
    }

    // Methods moved from Reservation
    public void updateStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void updateEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void updateRentalFee(BigDecimal rentalFee) {
        this.rentalFee = rentalFee;
    }

    public void updateDeposit(BigDecimal deposit) {
        this.deposit = deposit;
    }

    private void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수 값입니다.");
        }
    }

    private void validateQuantityLimit() {
        if (aiImages.size() >= 10) {
            throw new RentalImageUploadException("최대 등록 가능 이미지 수를 초과했습니다.");
        }
    }

    private void validateNoDuplicate(String imageUrl) {
        boolean exists = aiImages.stream()
                .anyMatch(img -> img.getImageUrl().equals(imageUrl));
        if (exists) {
            throw new IllegalArgumentException("이미 등록된 이미지 URL입니다: " + imageUrl);
        }
    }

    private RentalReservationProcess getRentalProcessForStatus(RentalReservationStatus status) {
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
