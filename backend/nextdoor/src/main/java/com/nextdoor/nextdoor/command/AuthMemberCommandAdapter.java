package com.nextdoor.nextdoor.command;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.auth.port.AuthMemberCommandPort;
import com.nextdoor.nextdoor.domain.auth.service.dto.MemberCommandDto;
import com.nextdoor.nextdoor.domain.auth.service.dto.MemberQueryDto;
import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import com.nextdoor.nextdoor.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Adapter
public class AuthMemberCommandAdapter implements AuthMemberCommandPort {

    private final MemberRepository memberRepository;

    @Override
    public MemberQueryDto save(MemberCommandDto command) {
        Member member = memberRepository.save(Member.builder()
                .authProvider(command.getAuthProvider())
                .nickname(command.getNickname())
                .email(command.getEmail())
                .profileImageUrl(command.getProfileImageUrl())
                .build());
        return new MemberQueryDto(member.getId(), member.getUuid(), member.getEmail());
    }
}
