package com.nextdoor.nextdoor.domain.member.service;

import com.nextdoor.nextdoor.domain.member.controller.dto.request.MemberExtraInfoSaveRequestDto;
import com.nextdoor.nextdoor.domain.member.controller.dto.response.MemberResponseDto;
import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import com.nextdoor.nextdoor.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    public MemberResponseDto updateMember(Long memberId, MemberExtraInfoSaveRequestDto memberDto) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.updateBirth(memberDto.getBirth());
        member.updateGender(memberDto.getGender());
        member.updateAddress(memberDto.getAddress());
        return MemberResponseDto.from(member);
    }

    @Override
    public MemberResponseDto retrieveMember(Long memberId) {
        return MemberResponseDto.from(memberRepository.findById(memberId).orElseThrow());
    }
}
