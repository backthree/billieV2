package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class ReservationUpdateRequestDto {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private BigDecimal rentalFee;

    @NotNull
    private BigDecimal deposit;
}