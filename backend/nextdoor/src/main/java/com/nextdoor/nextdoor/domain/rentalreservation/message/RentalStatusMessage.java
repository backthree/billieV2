package com.nextdoor.nextdoor.domain.rentalreservation.message;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RentalStatusMessage {

    private Long rentalId;
    private String process;
    private String detailStatus;
    private RentalDetailResult rentalDetail;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RentalDetailResult {
        private String postTitle;
        private String representativeImageUrl;
        private String ownerProfileImageUrl;
        private String renterProfileImageUrl;
    }
}
