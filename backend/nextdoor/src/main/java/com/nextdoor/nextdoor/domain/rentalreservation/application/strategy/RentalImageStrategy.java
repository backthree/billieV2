package com.nextdoor.nextdoor.domain.rentalreservation.application.strategy;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.entity.AiImageType;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.entity.RentalReservation;

public interface RentalImageStrategy {

    void updateRentalImage(RentalReservation rentalReservation, String imageUrl, String mimeType);
    AiImageType getImageType();
    String createImagePath(String rentalId);
    public void validateImageUploadAllowed(RentalReservation rentalReservation);
}
