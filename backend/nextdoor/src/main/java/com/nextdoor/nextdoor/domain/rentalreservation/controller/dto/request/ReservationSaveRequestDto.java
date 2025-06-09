package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ReservationSaveRequestDto {

    @NotNull
    private Long postId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}