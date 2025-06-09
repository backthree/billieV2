package com.nextdoor.nextdoor.domain.rentalreservation.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class AiImageComparisonPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_image_comparison_pair_id")
    private Long id;

    @NotNull
    private Long rentalId;

    @NotNull
    private Long beforeImageId;

    @NotNull
    private Long afterImageId;

    @Lob
    @Column(columnDefinition = "TEXT")
    @NotNull
    private String pairComparisonResult;

    public AiImageComparisonPair(Long rentalId, Long beforeImageId, Long afterImageId, String pairComparisonResult) {
        this.rentalId = rentalId;
        this.beforeImageId = beforeImageId;
        this.afterImageId = afterImageId;
        this.pairComparisonResult = pairComparisonResult;
    }
}
