package com.nextdoor.nextdoor.domain.rentalreservation.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class AiComparisonResult {

    private List<String> beforeImages;
    private List<String> afterImages;
    private String analysisResult;
    private String overallComparisonResult;
    private List<MatchingResult> matchingResults;

    @AllArgsConstructor
    @Getter
    public static class MatchingResult {

        private String beforeImage;
        private String afterImage;
        private String pairComparisonResult;
    }
}
