package com.nextdoor.nextdoor.domain.rentalreservation.service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRentalResult {

    private Long id;
    private Long postId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal rentalFee; //Per Day
    private BigDecimal deposit;
    private Long ownerId;
    private String ownerProfileImageUrl;
    private Long renterId;
    private String renterProfileImageUrl;
    private Long rentalId;
    private String rentalProcess;
    private String rentalStatus;
    private String title;
    private List<String> productImages;
    private LocalDateTime createdAt;
}
