package com.nextdoor.nextdoor.domain.rentalreservation.message;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
public class RequestRemittanceStatusMessage extends RentalStatusMessage{
    private BigDecimal rentalFee;
}
