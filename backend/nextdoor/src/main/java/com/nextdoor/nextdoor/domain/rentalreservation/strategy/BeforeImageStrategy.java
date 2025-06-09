package com.nextdoor.nextdoor.domain.rentalreservation.strategy;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.AiImageType;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationStatus;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.InvalidRentalStatusException;
import org.springframework.stereotype.Component;

@Component
public class BeforeImageStrategy implements RentalImageStrategy {

    @Override
    public void updateRentalImage(RentalReservation rentalReservation, String imageUrl, String mimeType) {
        validateImageUploadAllowed(rentalReservation);
        rentalReservation.saveAiImage(getImageType(), imageUrl, mimeType);
    }

    @Override
    public AiImageType getImageType() {
        return AiImageType.BEFORE;
    }

    @Override
    public String createImagePath(String rentalId) {
        return "rentals/" + rentalId + "/before";
    }

    @Override
    public void validateImageUploadAllowed(RentalReservation rentalReservation) {
        if(rentalReservation.getRentalReservationStatus() != RentalReservationStatus.REMITTANCE_REQUESTED) {
            throw new InvalidRentalStatusException("이미지 업로드가 불가능한 대여 상태입니다");
        }
    }
}