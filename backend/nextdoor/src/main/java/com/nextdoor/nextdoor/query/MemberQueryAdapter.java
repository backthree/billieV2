package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.fintech.port.MemberQueryPort;
import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import com.nextdoor.nextdoor.domain.member.domain.model.QMember;
import com.nextdoor.nextdoor.domain.member.domain.repository.MemberRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
@Adapter
public class MemberQueryAdapter implements MemberQueryPort {

    private final JPAQueryFactory jpaQueryFactory;
    private final MemberRepository memberRepository;
    private final QMember qMember = QMember.member;

    @Override
    public Optional<Member> findByUserKey(String userKey) {
        return memberRepository.findByUserKey(userKey);
    }

    @Override
    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }
}