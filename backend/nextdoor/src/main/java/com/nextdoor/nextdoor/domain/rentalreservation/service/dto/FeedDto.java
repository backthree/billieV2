package com.nextdoor.nextdoor.domain.rentalreservation.service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedDto {

    private Long id;
    private String title;
    private String productImageUrl;
}
