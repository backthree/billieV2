package com.nextdoor.nextdoor.domain.rentalreservation.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@Builder
@Getter
public class ReservationQueryDto {

    private Long reservationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal rentalFee;
    private BigDecimal deposit;
    private String status;
    private Long rentalId;
    private Long ownerId;
    private String ownerName;
    private String ownerProfileImageUrl;
    private Long renterId;
    private String renterName;
    private String renterProfileImageUrl;
    private Long postId;
    private String postTitle;
    private String postProductImage;
}