package com.nextdoor.nextdoor.domain.rentalreservation.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRentalCommand {

    private Long userId;
    private String userRole;
    private String condition;
    private Pageable pageable;
}
