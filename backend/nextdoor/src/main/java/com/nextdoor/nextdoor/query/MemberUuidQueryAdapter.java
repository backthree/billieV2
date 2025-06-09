package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.member.domain.QMember;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.QRentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.NoSuchRentalException;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.NoSuchReservationException;
import com.nextdoor.nextdoor.domain.rentalreservation.port.MemberUuidQueryPort;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Adapter
@RequiredArgsConstructor
public class MemberUuidQueryAdapter implements MemberUuidQueryPort {

    private final JPAQueryFactory queryFactory;
    private final QRentalReservation rentalReservation = QRentalReservation.rentalReservation;
    private final QMember member = QMember.member;

    @Override
    public String getMemberUuidByRentalIdAndRole(Long rentalId, String userRole) {
        RentalReservation foundRental = queryFactory
                .selectFrom(rentalReservation)
                .where(rentalReservation.id.eq(rentalId))
                .fetchOne();

        if (foundRental == null) {
            throw new NoSuchRentalException("대여 정보가 존재하지 않습니다. rentalId: " + rentalId);
        }

        Long memberId;
        if ("OWNER".equalsIgnoreCase(userRole)) {
            memberId = foundRental.getOwnerId();
        } else if ("RENTER".equalsIgnoreCase(userRole)) {
            memberId = foundRental.getRenterId();
        } else {
            throw new IllegalArgumentException("유효하지 않은 사용자 역할입니다. userRole: " + userRole);
        }

        if (memberId == null) {
            throw new NoSuchReservationException("예약 정보가 존재하지 않습니다. rentalId: " + rentalId);
        }
        String uuid = queryFactory
                .select(member.uuid)
                .from(member)
                .where(member.id.eq(memberId))
                .fetchOne();

        if (uuid == null) {
            throw new IllegalStateException("회원 정보가 존재하지 않습니다. memberId: " + memberId);
        }

        return uuid;
    }
}
