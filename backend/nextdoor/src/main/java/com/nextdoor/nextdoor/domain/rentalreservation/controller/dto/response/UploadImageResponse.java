package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class UploadImageResponse {

    private Long rentalId;
    private List<String> imageUrls;
    private LocalDateTime uploadedAt;
}
