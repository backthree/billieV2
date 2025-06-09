package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.aianalysis.enums.AiImageType;
import com.nextdoor.nextdoor.domain.aianalysis.port.AiAnalysisRentalQueryPort;
import com.nextdoor.nextdoor.domain.aianalysis.service.dto.RentalDto;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.QAiImage;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.QRentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservation;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
@Adapter
public class AiAnalysisRentalQueryAdapter implements AiAnalysisRentalQueryPort {

    private final JPAQueryFactory jpaQueryFactory;
    private final QRentalReservation qRental = QRentalReservation.rentalReservation;
    private final QAiImage qAiImage = QAiImage.aiImage;

    @Override
    public RentalDto findById(Long id) {
        RentalReservation rental = Optional.ofNullable(jpaQueryFactory.select(qRental)
                        .from(qRental)
                        .join(qAiImage)
                        .on(qRental.id.eq(qAiImage.rental.id))
                        .fetchJoin()
                        .where(qRental.id.eq(id))
                        .fetchOne())
                .orElseThrow();
        return RentalDto.builder()
                .rentalId(rental.getId())
                .reservationId(null) // Removed reservationId as it's now part of the same entity
                .rentalStatus(rental.getRentalReservationStatus().name())
                .damageAnalysis(rental.getDamageAnalysis())
                .comparedAnalysis(rental.getComparedAnalysis())
                .aiImages(rental.getAiImages().stream().map(aiImage -> RentalDto.AiImageDto.builder()
                        .aiImageId(aiImage.getId())
                        .type(AiImageType.valueOf(aiImage.getType().name()))
                        .imageUrl(aiImage.getImageUrl())
                        .mimeType(aiImage.getMimeType())
                        .rentalId(rental.getId())
                        .build()).toList())
                .build();
    }
}
