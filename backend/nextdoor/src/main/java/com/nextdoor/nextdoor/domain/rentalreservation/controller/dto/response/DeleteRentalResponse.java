package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteRentalResponse {
    private Long rentalId;
    private boolean success;
    private String message;
}