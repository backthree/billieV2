package com.nextdoor.nextdoor.domain.rentalreservation.event.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RequestRemittanceNotificationEvent {

    private Long rentalId;
    private Long renterId;
    private BigDecimal amount;
}
