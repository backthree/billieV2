package com.nextdoor.nextdoor.domain.rentalreservation.port;

import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.PostDto;

import java.util.Optional;

public interface RentalReservationPostQueryPort {

    Optional<PostDto> findById(Long postId);
}
