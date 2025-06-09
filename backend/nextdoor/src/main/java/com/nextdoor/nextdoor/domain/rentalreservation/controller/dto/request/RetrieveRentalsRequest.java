package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrieveRentalsRequest {

    @NotBlank(message = "사용자 역할은 필수입니다.")
    @Pattern(regexp = "^(OWNER|RENTER)$", message = "사용자 역할은 OWNER 또는 RENTER만 가능합니다.")
    private String userRole;

    @NotBlank(message = "조회 조건은 필수입니다.")
    @Pattern(regexp = "^(ALL|ACTIVE|COMPLETED)$", message = "조회 조건은 ALL, ACTIVE 또는 COMPLETED만 가능합니다.")
    private String condition;
}
