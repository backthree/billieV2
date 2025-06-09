package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RentalDetailResponse {

    private Long reservationId;
    private Long postId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal rentalFee; //Per Day
    private BigDecimal deposit;
    private String reservationStatus;
    private Long ownerId;
    private String ownerProfileImageUrl;
    private Long renterId;
    private String renterProfileImageUrl;
    private Long rentalId;
    private String rentalProcess;
    private String rentalStatus;
    private String title;
    private List<String> productImageUrls;
    private LocalDateTime createdAt;
}
