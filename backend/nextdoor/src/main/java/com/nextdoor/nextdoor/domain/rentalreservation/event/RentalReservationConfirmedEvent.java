package com.nextdoor.nextdoor.domain.rentalreservation.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalReservationConfirmedEvent {
    private Long rentalReservationId;
}