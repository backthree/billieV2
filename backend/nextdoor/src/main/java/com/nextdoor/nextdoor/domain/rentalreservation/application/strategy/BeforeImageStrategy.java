package com.nextdoor.nextdoor.domain.rentalreservation.application.strategy;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.AiImageType;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.RentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.RentalReservationStatus;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.exception.InvalidRentalStatusException;
import org.springframework.stereotype.Component;

@Component
public class BeforeImageStrategy implements RentalImageStrategy {

    @Override
    public void updateRentalImage(RentalReservation rentalReservation, String imageUrl, String mimeType) {
        validateImageUploadAllowed(rentalReservation);
        rentalReservation.addAiImage(getImageType(), imageUrl, mimeType);
    }

    @Override
    public AiImageType getImageType() {
        return AiImageType.BEFORE;
    }

    @Override
    public void validateImageUploadAllowed(RentalReservation rentalReservation) {
        if(rentalReservation.getRentalReservationStatus() != RentalReservationStatus.REMITTANCE_REQUESTED) {
            throw new InvalidRentalStatusException("이미지 업로드가 불가능한 대여 상태입니다");
        }
    }
}