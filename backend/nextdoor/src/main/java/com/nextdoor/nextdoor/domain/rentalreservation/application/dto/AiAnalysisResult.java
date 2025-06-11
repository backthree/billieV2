package com.nextdoor.nextdoor.domain.rentalreservation.application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiAnalysisResult {

    private List<String> beforeImages;
    private List<String> afterImages;
    private String analysis;
}
