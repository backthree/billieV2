package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.chat.dto.MemberDto;
import com.nextdoor.nextdoor.domain.chat.port.ChatMemberQueryPort;
import com.nextdoor.nextdoor.domain.member.domain.model.QMember;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
@Adapter
public class ChatMemberQueryAdapter implements ChatMemberQueryPort {

    private final JPAQueryFactory jpaQueryFactory;
    private final QMember qMember = QMember.member;

    @Override
    public Optional<MemberDto> findById(Long memberId) {
        return Optional.ofNullable(jpaQueryFactory.select(
                Projections.constructor(
                    MemberDto.class,
                    qMember.id,
                    qMember.nickname,
                    qMember.profileImageUrl
                ))
                .from(qMember)
                .where(qMember.id.eq(memberId))
                .fetchOne());
    }
}