package com.nextdoor.nextdoor.domain.rentalreservation.exception;

import com.nextdoor.nextdoor.domain.aianalysis.exception.BaseCustomException;
import lombok.Getter;

@Getter
public class PairComparisonProcessingException extends BaseCustomException {

    private final String errorCode = "PAIR_COMPARISON_PROCESSING_EXCEPTION";

    public PairComparisonProcessingException() {
        super();
    }

    public PairComparisonProcessingException(String message) {
        super(message);
    }

    public PairComparisonProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PairComparisonProcessingException(Throwable cause) {
        super(cause);
    }
}
