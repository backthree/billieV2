package com.nextdoor.nextdoor.domain.rentalreservation.presentation.controller;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.AiComparisonResult;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.UploadImageCommand;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.UploadImageResult;
import com.nextdoor.nextdoor.domain.rentalreservation.application.service.RentalImageAnalysisService;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.request.UploadImageRequest;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.AiComparisonResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.UploadImageResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.mapper.RentalReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
public class RentalImageController {

    private final RentalImageAnalysisService rentalImageAnalysisService;
    private final RentalReservationMapper rentalReservationMapper;

    @PostMapping("/{rentalId}/before/photos")
    public ResponseEntity<UploadImageResponse> registerBeforePhoto(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long rentalId,
            @RequestParam("file") List<MultipartFile> images) {

        UploadImageRequest request = UploadImageRequest.builder().images(images).build();
        UploadImageCommand command = rentalReservationMapper.toUploadImageCommand(userId, rentalId, request);
        UploadImageResult result = rentalImageAnalysisService.registerBeforeImage(command);
        UploadImageResponse response = rentalReservationMapper.toUploadImageResponse(result);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rentalId}/after/photos")
    public ResponseEntity<UploadImageResponse> registerAfterPhoto(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long rentalId,
            @RequestParam("file") List<MultipartFile> images) {

        UploadImageRequest request = UploadImageRequest.builder().images(images).build();
        UploadImageCommand command = rentalReservationMapper.toUploadImageCommand(userId, rentalId, request);
        UploadImageResult result = rentalImageAnalysisService.registerAfterImage(command);
        UploadImageResponse response = rentalReservationMapper.toUploadImageResponse(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rentalId}/ai-analysis")
    public ResponseEntity<AiComparisonResponse> getAiAnalysis(@PathVariable Long rentalId){
        AiComparisonResult result = rentalImageAnalysisService.getAiAnalysis(rentalId);
        AiComparisonResponse response = rentalReservationMapper.toResponse(result);

        return ResponseEntity.ok(response);
    }
}