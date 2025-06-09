package com.nextdoor.nextdoor.domain.rentalreservation.service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDto {

    private Long reservationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal rentalFee; //Per Day
    private BigDecimal deposit;
    private String reservationStatus;
    private Long ownerId;
    private Long renterId;
    private Long feedId;
}
