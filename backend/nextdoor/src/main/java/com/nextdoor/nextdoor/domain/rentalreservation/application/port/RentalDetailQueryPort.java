package com.nextdoor.nextdoor.domain.rentalreservation.application.port;

import com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.message.RentalStatusMessage;

public interface RentalDetailQueryPort {

    RentalStatusMessage.RentalDetailResult getRentalDetailByRentalIdAndRole(Long rentalId);
}