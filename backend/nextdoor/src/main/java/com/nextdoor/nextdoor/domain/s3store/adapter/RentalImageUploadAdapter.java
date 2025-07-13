package com.nextdoor.nextdoor.domain.s3store.adapter;

import com.nextdoor.nextdoor.domain.rentalreservation.application.port.S3ImageUploadPort;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.S3UploadResult;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.AiImageType;
import com.nextdoor.nextdoor.domain.s3store.dto.FileUploadRequestDto;
import com.nextdoor.nextdoor.domain.s3store.dto.FileUploadResponseDto;
import com.nextdoor.nextdoor.domain.s3store.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class RentalImageUploadAdapter implements S3ImageUploadPort {

    private final S3Service s3Service;

    @Override
    public S3UploadResult upload(MultipartFile file, Long entityId, String category) {
        String path = createImagePath(entityId, category);

        FileUploadRequestDto requestDto = FileUploadRequestDto.builder()
                .file(file)
                .directory(path)
                .serviceId("rental-service")
                .resourceId("image")
                .resourceType("rental-image")
                .build();

        FileUploadResponseDto responseDto = s3Service.uploadFile(requestDto);

        return new S3UploadResult(
                responseDto.getFileUrl(),
                responseDto.getFileName()
        );
    }

    private String createImagePath(Long entityId, String category) {
        return "rentals/" + entityId + "/" + category.toLowerCase();
    }
}