package com.nextdoor.nextdoor.domain.member.service;

import com.nextdoor.nextdoor.domain.member.controller.dto.request.MemberExtraInfoSaveRequestDto;
import com.nextdoor.nextdoor.domain.member.controller.dto.response.MemberResponseDto;
import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import com.nextdoor.nextdoor.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    public MemberResponseDto updateMember(Long memberId, MemberExtraInfoSaveRequestDto memberDto) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        if (memberDto.getBirth() != null) {
            member.updateBirth(memberDto.getBirth());
        }
        if (memberDto.getGender() != null) {
            member.updateGender(memberDto.getGender());
        }
        if (memberDto.getAddress() != null) {
            member.updateAddress(memberDto.getAddress());
        }
//        if (memberDto.getAccountId() != null) {
//            member.updateAccountId(memberDto.getAccountId());
//        }
        return MemberResponseDto.from(member);
    }

    @Override
    public MemberResponseDto retrieveMember(Long memberId) {
        return MemberResponseDto.from(memberRepository.findById(memberId).orElseThrow());
    }
}
