package com.nextdoor.nextdoor.domain.rentalreservation.application.service;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.*;
import org.springframework.stereotype.Service;

/**
 * 대여 이미지 및 AI 분석 서비스
 * 책임: 대여 전/후 사진 등록, AI 분석 요청 및 결과 업데이트, 이미지 비교 쌍 관리에 집중합니다.
 */
@Service
public interface RentalImageAnalysisService {

    /**
     * 대여 전 사진을 등록합니다.
     */
    UploadImageResult registerBeforeImage(UploadImageCommand command);

    /**
     * 대여 후 사진을 등록합니다.
     */
    UploadImageResult registerAfterImage(UploadImageCommand command);

    /**
     * AI 분석 결과를 조회합니다.
     */
    AiComparisonResult getAiAnalysis(Long rentalId);

    /**
     * 손상 분석 결과를 업데이트합니다.
     */
    void updateDamageAnalysis(Long rentalId, String damageAnalysis);

    /**
     * 비교 분석 결과를 업데이트합니다.
     */
    void updateComparedAnalysis(Long rentalId, String comparedAnalysis);

    /**
     * AI 이미지 비교 쌍을 생성합니다.
     */
    void createAiImageComparisonPair(Long rentalId, Long beforeImageId, Long afterImageId, String pairComparisonResult);

    /**
     * 대여 ID에 해당하는 AI 이미지 비교 쌍을 삭제합니다.
     */
    void deleteAiImageComparisonPairByRentalId(Long rentalId);
}