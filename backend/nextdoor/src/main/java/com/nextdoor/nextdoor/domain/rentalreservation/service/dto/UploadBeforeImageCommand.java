package com.nextdoor.nextdoor.domain.rentalreservation.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
public class UploadBeforeImageCommand {

    private Long rentalId;
    private MultipartFile file;
}
