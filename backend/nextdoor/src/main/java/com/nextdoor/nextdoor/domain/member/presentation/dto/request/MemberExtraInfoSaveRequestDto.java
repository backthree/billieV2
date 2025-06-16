package com.nextdoor.nextdoor.domain.member.presentation.dto.request;

import com.nextdoor.nextdoor.domain.member.domain.model.Gender;
import lombok.Getter;

@Getter
public class MemberExtraInfoSaveRequestDto {

    private String birth;
    private Gender gender;
    private String address;
    private Long accountId;
}
