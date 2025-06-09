package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemittanceResponse {

    private String ownerNickname;
    private String ownerProfileImageUrl;
    private BigDecimal rentalFee;
    private BigDecimal deposit;
    private String accountNo;
    private String bankCode;
    private LocalDate startDate;
    private LocalDate endDate;
}
