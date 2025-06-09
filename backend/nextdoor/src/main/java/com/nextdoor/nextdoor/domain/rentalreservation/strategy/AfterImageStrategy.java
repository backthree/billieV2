package com.nextdoor.nextdoor.domain.rentalreservation.strategy;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.AiImageType;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationStatus;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.InvalidRentalStatusException;
import org.springframework.stereotype.Component;

@Component
public class AfterImageStrategy implements RentalImageStrategy {

    @Override
    public void updateRentalImage(RentalReservation rentalReservation, String imageUrl, String mimeType) {
        validateImageUploadAllowed(rentalReservation);
        rentalReservation.saveAiImage(getImageType(), imageUrl, mimeType);
    }

    @Override
    public AiImageType getImageType() {
        return AiImageType.AFTER;
    }

    @Override
    public String createImagePath(String rentalId) {
        return "rentals/" + rentalId + "/after";
    }

    @Override
    public void validateImageUploadAllowed(RentalReservation rentalReservation) {
        if(rentalReservation.getRentalReservationStatus() != RentalReservationStatus.RENTAL_PERIOD_ENDED){
            throw new InvalidRentalStatusException("대여 이미지 업로드 불가능.");
        }
    }
}