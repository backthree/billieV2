package com.nextdoor.nextdoor.domain.rentalreservation.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ReservationMemberQueryDto {

    private Long memberId;
    private String nickname;
    private String profileImageUrl;
}