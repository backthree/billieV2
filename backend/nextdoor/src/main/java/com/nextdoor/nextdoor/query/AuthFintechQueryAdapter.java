package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.auth.port.AuthFintechQueryPort;
import com.nextdoor.nextdoor.domain.auth.dto.AuthFintechQueryDto;
import com.nextdoor.nextdoor.domain.member.domain.model.QMember;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Adapter
public class AuthFintechQueryAdapter implements AuthFintechQueryPort {

    private final JPAQueryFactory jpaQueryFactory;
    private final QMember qMember = QMember.member;

    @Override
    public AuthFintechQueryDto findByUserId(Long userId) {
        return jpaQueryFactory.select(Projections.constructor(
                        AuthFintechQueryDto.class,
                        qMember.userKey
                ))
                .from(qMember)
                .where(qMember.id.eq(userId))
                .fetchOne();
    }
}
