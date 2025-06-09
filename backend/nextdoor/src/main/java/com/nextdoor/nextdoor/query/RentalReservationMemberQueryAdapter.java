package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.member.domain.QMember;
import com.nextdoor.nextdoor.domain.rentalreservation.port.ReservationMemberQueryPort;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.ReservationMemberQueryDto;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
@Adapter
public class RentalReservationMemberQueryAdapter implements ReservationMemberQueryPort {

    private final JPAQueryFactory jpaQueryFactory;

    private final QMember qMember = QMember.member;

    @Override
    public Optional<ReservationMemberQueryDto> findById(Long id) {
        return Optional.ofNullable(jpaQueryFactory.select(createReservationMemberQueryDtoProjection())
                .from(qMember)
                .where(qMember.id.eq(id))
                .fetchOne());
    }

    private ConstructorExpression<ReservationMemberQueryDto> createReservationMemberQueryDtoProjection() {
        return Projections.constructor(
                ReservationMemberQueryDto.class,
                qMember.id,
                qMember.nickname,
                qMember.profileImageUrl
        );
    }
}