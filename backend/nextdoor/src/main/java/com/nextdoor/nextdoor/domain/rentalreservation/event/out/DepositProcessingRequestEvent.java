package com.nextdoor.nextdoor.domain.rentalreservation.event.out;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepositProcessingRequestEvent {

    private Long rentalId;
}
