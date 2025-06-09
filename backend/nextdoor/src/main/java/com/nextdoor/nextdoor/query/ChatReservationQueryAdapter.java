package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.chat.dto.ReservationDto;
import com.nextdoor.nextdoor.domain.chat.port.ChatReservationQueryPort;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.QRentalReservation;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
@Adapter
public class ChatReservationQueryAdapter implements ChatReservationQueryPort {

    private final JPAQueryFactory jpaQueryFactory;
    private final QRentalReservation qRental = QRentalReservation.rentalReservation;

    @Override
    public Optional<ReservationDto> findByPostIdAndOwnerIdAndRenterId(Long postId, Long ownerId, Long renterId) {
        return Optional.ofNullable(jpaQueryFactory.select(
                        Projections.constructor(
                                ReservationDto.class,
                                qRental.id,
                                qRental.rentalReservationStatus,
                                qRental.id
                        ))
                .from(qRental)
                .where(qRental.postId.eq(postId)
                        .and(qRental.ownerId.eq(ownerId))
                        .and(qRental.renterId.eq(renterId)))
                .orderBy(qRental.id.desc())
                .fetchFirst());
    }
}
