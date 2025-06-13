package com.nextdoor.nextdoor.domain.member.presentation.dto.response;

import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import com.nextdoor.nextdoor.domain.member.enums.Gender;
import lombok.Getter;

@Getter
public class MemberResponseDto {

    private Long id;
    private String uuid;
    private String userKey;
    private String email;
    private String birth;
    private Gender gender;
    private String address;
    private String profileImageUrl;
    private String nickname;
    private String authProvider;

    public static MemberResponseDto from(Member member) {
        MemberResponseDto dto = new MemberResponseDto();
        dto.id = member.getId();
        dto.uuid = member.getUuid();
        dto.userKey = member.getUserKey();
        dto.email = member.getEmail();
        dto.birth = member.getBirth();
        dto.gender = member.getGender();
        dto.address = member.getAddress();
        dto.profileImageUrl = member.getProfileImageUrl();
        dto.nickname = member.getNickname();
        dto.authProvider = member.getAuthProvider();
        return dto;
    }
}
