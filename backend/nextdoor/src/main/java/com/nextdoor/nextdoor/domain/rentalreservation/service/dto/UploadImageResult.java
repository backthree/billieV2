package com.nextdoor.nextdoor.domain.rentalreservation.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class UploadImageResult {

    private Long rentalId;
    private String type;
    private List<String> imageUrls;
    private LocalDateTime uploadedAt;
}
