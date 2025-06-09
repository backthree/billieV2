package com.nextdoor.nextdoor.domain.rentalreservation.service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestRemittanceResult {

    private String ownerNickname;
    private String ownerProfileImageUrl;
    private BigDecimal finalAmount;
    private BigDecimal deposit;
    private String accountNo;
    private String bankCode;
    private LocalDate startDate;
    private LocalDate endDate;
}
