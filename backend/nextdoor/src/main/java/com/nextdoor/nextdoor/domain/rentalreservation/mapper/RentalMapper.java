package com.nextdoor.nextdoor.domain.rentalreservation.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request.RetrieveRentalsRequest;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request.UpdateAccountRequest;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request.UploadImageRequest;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response.AiComparisonResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response.DeleteRentalResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response.ManagedRentalCountResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response.RemittanceResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response.RentalDetailResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response.UpdateAccountResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response.UploadImageResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.PairComparisonProcessingException;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class RentalMapper {

    private final ObjectMapper objectMapper;

    public UploadImageCommand toUploadImageCommand(Long userId, Long rentalId, UploadImageRequest request) {
        return new UploadImageCommand(
                userId,
                rentalId,
                request.getImages()
        );
    }

    public UploadImageResponse toUploadImageResponse(UploadImageResult result) {
        return UploadImageResponse.builder()
                .rentalId(result.getRentalId())
                .imageUrls(result.getImageUrls())
                .uploadedAt(result.getUploadedAt())
                .build();
    }

    public RequestRemittanceCommand toCommand(Long rentalId) {
        return RequestRemittanceCommand.builder()
                .rentalId(rentalId)
                .build();
    }

    public RemittanceResponse toResponse(RequestRemittanceResult result) {
        return RemittanceResponse.builder()
                .ownerNickname(result.getOwnerNickname())
                .ownerProfileImageUrl(result.getOwnerProfileImageUrl())
                .rentalFee(result.getFinalAmount())
                .deposit(result.getDeposit())
                .accountNo(result.getAccountNo())
                .bankCode(result.getBankCode())
                .startDate(result.getStartDate())
                .endDate(result.getEndDate())
                .build();
    }

    public SearchRentalCommand toCommand(Long userId,
                                               RetrieveRentalsRequest request,
                                               Pageable pageable) {
        return SearchRentalCommand.builder()
                .userId(userId)
                .userRole(request.getUserRole())
                .condition(request.getCondition())
                .pageable(pageable)
                .build();
    }

    public RentalDetailResponse toResponse(SearchRentalResult result) {
        return RentalDetailResponse.builder()
                .reservationId(result.getId())
                .postId(result.getPostId())
                .startDate(result.getStartDate())
                .endDate(result.getEndDate())
                .rentalFee(result.getRentalFee())
                .deposit(result.getDeposit())
                .ownerId(result.getOwnerId())
                .ownerProfileImageUrl(result.getOwnerProfileImageUrl())
                .renterId(result.getRenterId())
                .renterProfileImageUrl(result.getRenterProfileImageUrl())
                .rentalId(result.getRentalId())
                .rentalProcess(result.getRentalProcess())
                .rentalStatus(result.getRentalStatus())
                .title(result.getTitle())
                .productImageUrls(result.getProductImages())
                .createdAt(result.getCreatedAt())
                .build();
    }

    public AiComparisonResponse toResponse(AiComparisonResult result) {
        return AiComparisonResponse.builder()
                .beforeImages(result.getBeforeImages())
                .afterImages(result.getAfterImages())
                .analysisResult(result.getAnalysisResult())
                .overallComparisonResult(result.getOverallComparisonResult())
                .matchingResults(result.getMatchingResults().stream()
                        .map(this::toMatchingResult)
                        .toList())
                .build();
    }

    private AiComparisonResponse.MatchingResult toMatchingResult(AiComparisonResult.MatchingResult source) {
        try {
            return AiComparisonResponse.MatchingResult.builder()
                    .beforeImage(source.getBeforeImage())
                    .afterImage(source.getAfterImage())
                    .pairComparisonResult(
                            objectMapper.readValue(source.getPairComparisonResult(),
                            AiComparisonResponse.MatchingResult.PairComparisonResult.class))
                    .build();
        } catch (JsonProcessingException e) {
            throw new PairComparisonProcessingException("비교 결과 처리 도중 예외 발생", e);
        }
    }

    public UpdateAccountCommand toUpdateAccountCommand(Long rentalId, UpdateAccountRequest request) {
        return UpdateAccountCommand.builder()
                .rentalId(rentalId)
                .accountNo(request.getAccountNo())
                .bankCode(request.getBankCode())
                .finalAmount(request.getFinalAmount())
                .build();
    }

    public UpdateAccountResponse toUpdateAccountResponse(UpdateAccountResult result) {
        return UpdateAccountResponse.builder()
                .rentalId(result.getRentalId())
                .accountNo(result.getAccountNo())
                .bankCode(result.getBankCode())
                .finalAmount(result.getFinalAmount())
                .build();
    }

    public ManagedRentalCountResponse toManagedRentalCountResponse(ManagedRentalCountResult result) {
        return ManagedRentalCountResponse.builder()
                .managedRentalCount(result.getManagedRentalCount())
                .build();
    }

    public DeleteRentalCommand toDeleteCommand(Long rentalId) {
        return DeleteRentalCommand.builder()
                .rentalId(rentalId)
                .build();
    }

    public DeleteRentalResponse toDeleteResponse(DeleteRentalResult result) {
        return DeleteRentalResponse.builder()
                .rentalId(result.getRentalId())
                .success(result.isSuccess())
                .message(result.getMessage())
                .build();
    }
}
