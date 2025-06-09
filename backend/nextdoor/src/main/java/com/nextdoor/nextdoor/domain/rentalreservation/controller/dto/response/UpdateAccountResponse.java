package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountResponse {
    private Long rentalId;
    private String accountNo;
    private String bankCode;
    private BigDecimal finalAmount;
}
