package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AiComparisonResponse {

    private List<String> beforeImages;
    private List<String> afterImages;
    private String analysisResult;
    private String overallComparisonResult;
    private List<MatchingResult> matchingResults;

    @Builder
    @Getter
    public static class MatchingResult {

        private String beforeImage;
        private String afterImage;
        private PairComparisonResult pairComparisonResult;

        @Builder
        @Getter
        public static class PairComparisonResult {

            private String result;
            private List<Damage> damages;

            @Builder
            @Getter
            public static class Damage {

                private String damageType;
                private String location;
                private String details;
                private BoundingBox boundingBox;
                private float confidenceScore;

                @Builder
                @Getter
                public static class BoundingBox {

                    private float xMin;
                    private float yMin;
                    private float xMax;
                    private float yMax;
                }
            }
        }
    }
}
