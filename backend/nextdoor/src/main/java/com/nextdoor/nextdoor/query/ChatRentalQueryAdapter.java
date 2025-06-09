package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.chat.dto.RentalDto;
import com.nextdoor.nextdoor.domain.chat.port.ChatRentalQueryPort;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.QRentalReservation;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
@Adapter
public class ChatRentalQueryAdapter implements ChatRentalQueryPort {

    private final JPAQueryFactory jpaQueryFactory;
    private final QRentalReservation qRental = QRentalReservation.rentalReservation;

    @Override
    public Optional<RentalDto> findById(Long rentalId) {
        return Optional.ofNullable(jpaQueryFactory.select(
                        Projections.constructor(
                                RentalDto.class,
                                qRental.id,
                                qRental.rentalReservationProcess
                        ))
                .from(qRental)
                .where(qRental.id.eq(rentalId))
                .fetchOne());
    }
}
