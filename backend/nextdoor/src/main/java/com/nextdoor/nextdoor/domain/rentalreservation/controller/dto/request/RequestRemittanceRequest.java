package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestRemittanceRequest {

    @NotNull(message = "대여자 ID는 필수입니다.")
    private Long renterId;
}
