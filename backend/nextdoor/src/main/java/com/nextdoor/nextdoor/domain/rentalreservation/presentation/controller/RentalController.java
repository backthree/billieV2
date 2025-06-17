package com.nextdoor.nextdoor.domain.rentalreservation.presentation.controller;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.*;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.request.RetrieveRentalsRequest;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.request.UpdateAccountRequest;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.request.UploadImageRequest;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.AiComparisonResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.DeleteRentalResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.ManagedRentalCountResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.RemittanceResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.RentalDetailResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.UpdateAccountResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.UploadImageResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.mapper.RentalReservationMapper;
import com.nextdoor.nextdoor.domain.rentalreservation.application.service.RentalImageAnalysisService;
import com.nextdoor.nextdoor.domain.rentalreservation.application.service.RentalQueryService;
import com.nextdoor.nextdoor.domain.rentalreservation.application.service.RentalSettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalImageAnalysisService rentalImageAnalysisService;
    private final RentalSettlementService rentalSettlementService;
    private final RentalQueryService rentalQueryService;
    private final RentalReservationMapper rentalReservationMapper;

    @GetMapping
    public ResponseEntity<Page<RentalDetailResponse>> getMyRentals(
            @RequestParam Long userId,
            @RequestParam String userRole,
            @RequestParam String condition,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        RetrieveRentalsRequest retrieveRentalsRequest = new RetrieveRentalsRequest(userRole, condition);
        SearchRentalCommand command = rentalReservationMapper.toCommand(userId, retrieveRentalsRequest, pageable);
        Page<SearchRentalResult> results = rentalQueryService.searchRentals(command);
        Page<RentalDetailResponse> responsePage = results.map(rentalReservationMapper::toResponse);

        return ResponseEntity.ok(responsePage);
    }

    @PostMapping("/{rentalId}/before/photos")
    public ResponseEntity<UploadImageResponse> registerBeforePhoto(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long rentalId,
            @RequestParam("file") List<MultipartFile> images) {

        UploadImageRequest request = UploadImageRequest.builder()
                .images(images)
                .build();

        UploadImageCommand command = rentalReservationMapper.toUploadImageCommand(userId, rentalId, request);
        UploadImageResult result = rentalImageAnalysisService.registerBeforePhoto(command);
        UploadImageResponse response = rentalReservationMapper.toUploadImageResponse(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rentalId}/request-remittance")
    public ResponseEntity<RemittanceResponse> requestRemittance(@PathVariable Long rentalId) {
        RequestRemittanceCommand command = rentalReservationMapper.toCommand(rentalId);
        RequestRemittanceResult result = rentalSettlementService.requestRemittance(command);
        RemittanceResponse response = rentalReservationMapper.toResponse(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rentalId}/remittance-data")
    public ResponseEntity<RemittanceResponse> getRemittanceData(@PathVariable Long rentalId) {
        RequestRemittanceResult result = rentalSettlementService.getRemittanceData(rentalId);
        RemittanceResponse response = rentalReservationMapper.toResponse(result);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rentalId}/after/photos")
    public ResponseEntity<UploadImageResponse> registerAfterPhoto(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long rentalId,
            @RequestParam("file") List<MultipartFile> images) {

        UploadImageRequest request = UploadImageRequest.builder()
                .images(images)
                .build();

        UploadImageCommand command = rentalReservationMapper.toUploadImageCommand(userId, rentalId, request);
        UploadImageResult result = rentalImageAnalysisService.registerAfterPhoto(command);
        UploadImageResponse response = rentalReservationMapper.toUploadImageResponse(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rentalId}/ai-analysis")
    ResponseEntity<AiComparisonResponse> getAiAnalysis(@PathVariable Long rentalId){
        AiComparisonResult result = rentalImageAnalysisService.getAiAnalysis(rentalId);
        AiComparisonResponse response = rentalReservationMapper.toResponse(result);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rentalId}/account")
    public ResponseEntity<UpdateAccountResponse> updateAccount(
            @PathVariable Long rentalId,
            @Valid @RequestBody UpdateAccountRequest request) {

        UpdateAccountCommand command = rentalReservationMapper.toUpdateAccountCommand(rentalId, request);
        UpdateAccountResult result = rentalSettlementService.updateAccount(command);
        UpdateAccountResponse response = rentalReservationMapper.toUpdateAccountResponse(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/managed-count")
    public ResponseEntity<ManagedRentalCountResponse> getManagedRentalCount(
            @RequestParam Long userId) {

        ManagedRentalCountResult result = rentalQueryService.countManagedRentals(userId);
        ManagedRentalCountResponse response = rentalReservationMapper.toManagedRentalCountResponse(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rentalId}")
    public ResponseEntity<RentalDetailResponse> getRentalById(
            @PathVariable Long rentalId) {

        SearchRentalResult result = rentalQueryService.getRentalById(rentalId);
        RentalDetailResponse response = rentalReservationMapper.toResponse(result);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{rentalId}")
    public ResponseEntity<DeleteRentalResponse> deleteRental(
            @PathVariable Long rentalId) {

        DeleteRentalCommand command = rentalReservationMapper.toDeleteCommand(rentalId);
        DeleteRentalResult result = rentalQueryService.deleteRental(command);
        DeleteRentalResponse response = rentalReservationMapper.toDeleteResponse(result);

        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
