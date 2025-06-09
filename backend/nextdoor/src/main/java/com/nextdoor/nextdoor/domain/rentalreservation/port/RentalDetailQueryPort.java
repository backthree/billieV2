package com.nextdoor.nextdoor.domain.rentalreservation.port;

import com.nextdoor.nextdoor.domain.rentalreservation.message.RentalStatusMessage;

public interface RentalDetailQueryPort {

    RentalStatusMessage.RentalDetailResult getRentalDetailByRentalIdAndRole(Long rentalId);
}