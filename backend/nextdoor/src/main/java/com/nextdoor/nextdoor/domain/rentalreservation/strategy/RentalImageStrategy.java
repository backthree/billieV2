package com.nextdoor.nextdoor.domain.rentalreservation.strategy;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.AiImageType;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservation;

public interface RentalImageStrategy {

    void updateRentalImage(RentalReservation rentalReservation, String imageUrl, String mimeType);
    AiImageType getImageType();
    String createImagePath(String rentalId);
    public void validateImageUploadAllowed(RentalReservation rentalReservation);
}
