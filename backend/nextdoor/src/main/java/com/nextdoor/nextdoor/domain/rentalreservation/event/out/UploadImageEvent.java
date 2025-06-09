package com.nextdoor.nextdoor.domain.rentalreservation.event.out;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@AllArgsConstructor
public class UploadImageEvent {

    private MultipartFile image;
}
