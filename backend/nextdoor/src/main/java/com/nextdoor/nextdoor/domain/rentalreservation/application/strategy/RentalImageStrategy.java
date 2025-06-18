package com.nextdoor.nextdoor.domain.rentalreservation.application.strategy;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.AiImageType;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.RentalReservation;

public interface RentalImageStrategy {

    void updateRentalImage(RentalReservation rentalReservation, String imageUrl, String mimeType);
    AiImageType getImageType();
    void validateImageUploadAllowed(RentalReservation rentalReservation);
}
