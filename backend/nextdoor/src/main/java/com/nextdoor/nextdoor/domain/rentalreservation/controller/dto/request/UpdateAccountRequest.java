package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {
    @NotBlank(message = "계좌번호는 필수입니다.")
    private String accountNo;

    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    @NotNull(message = "최종 금액은 필수입니다.")
    @Positive(message = "최종 금액은 0보다 커야 합니다.")
    private BigDecimal finalAmount;
}
