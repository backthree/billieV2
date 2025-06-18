package com.nextdoor.nextdoor.domain.rentalreservation.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Period {

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    public Period(LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("시작일은 필수 값입니다.");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("종료일은 필수 값입니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("대여 시작일은 종료일보다 빠르거나 같아야 합니다.");
        }
    }
}