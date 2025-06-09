package com.nextdoor.nextdoor.domain.rentalreservation.event.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalCreatedEvent {

    private Long rentalId;
    private Long reservationId;
}