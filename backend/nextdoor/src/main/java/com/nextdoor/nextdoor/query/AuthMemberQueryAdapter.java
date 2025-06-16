package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.auth.port.AuthMemberQueryPort;
import com.nextdoor.nextdoor.domain.auth.service.dto.MemberQueryDto;
import com.nextdoor.nextdoor.domain.member.domain.model.QMember;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
@Adapter
public class AuthMemberQueryAdapter implements AuthMemberQueryPort {

    private final JPAQueryFactory jpaQueryFactory;

    private final QMember qMember = QMember.member;

    @Override
    public Optional<MemberQueryDto> findById(Long id) {
        return Optional.ofNullable(jpaQueryFactory.select(Projections.constructor(
                        MemberQueryDto.class,
                        qMember.id,
                        qMember.uuid,
                        qMember.email
                ))
                .from(qMember)
                .where(qMember.id.eq(id))
                .fetchOne());
    }

    @Override
    public Optional<MemberQueryDto> findByEmailAndAuthProvider(String email, String authProvider) {
        return Optional.ofNullable(jpaQueryFactory.select(Projections.constructor(
                        MemberQueryDto.class,
                        qMember.id,
                        qMember.uuid,
                        qMember.email
                ))
                .from(qMember)
                .where(qMember.email.eq(email).and(qMember.authProvider.eq(authProvider)))
                .fetchOne());
    }
}
