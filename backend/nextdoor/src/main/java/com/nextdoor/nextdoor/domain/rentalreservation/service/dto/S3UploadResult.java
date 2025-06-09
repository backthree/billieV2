package com.nextdoor.nextdoor.domain.rentalreservation.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class S3UploadResult {
    private final String url;
    private final String key;
}