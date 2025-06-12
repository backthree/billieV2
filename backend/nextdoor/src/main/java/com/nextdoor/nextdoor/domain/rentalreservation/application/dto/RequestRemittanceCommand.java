package com.nextdoor.nextdoor.domain.rentalreservation.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RequestRemittanceCommand {

    private Long rentalId;
}
