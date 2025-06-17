package com.nextdoor.nextdoor.domain.rentalreservation.domain.model;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.exception.RentalImageUploadException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiImages {
    private static int MAX_SIZE = 10;
    private List<AiImage> images = new ArrayList<>();

    public AiImages(List<AiImage> images) {
        if (images != null) {
            this.images = new ArrayList<>(images);
            validateQuantityLimit();
            validateNoDuplicates();
        }
    }

    public void addImage(AiImage newImage) {
        if (newImage == null) {
            throw new IllegalArgumentException("추가할 이미지는 null이 될 수 없습니다.");
        }
        validateQuantityLimitBeforeAdd();
        validateNoDuplicate(newImage.getImageUrl());
    }

    private void validateQuantityLimit() {
        if (images.size() > MAX_SIZE) {
            throw new RentalImageUploadException("최대 등록 가능 이미지 수를 초과했습니다.");
        }
    }


    private void validateQuantityLimitBeforeAdd() {
        if (images.size() >= MAX_SIZE) {
            throw new RentalImageUploadException("최대 등록 가능 이미지 수를 초과했습니다.");
        }
    }

    /**
     * 생성자에서 초기 리스트를 받을 때 사용됩니다.
     */
    private void validateNoDuplicates() {
        long distinctCount = images.stream()
                .map(AiImage::getImageUrl)
                .distinct()
                .count();
        if (distinctCount != images.size()) {
            throw new IllegalArgumentException("중복된 이미지 URL이 존재합니다.");
        }
    }

    /**
     * * 새로운 이미지를 추가하기 전에 사용됩니다.
     */
    private void validateNoDuplicate(String imageUrl) {
        boolean exists = images.stream()
                .anyMatch(img -> img.getImageUrl().equals(imageUrl));
        if (exists) {
            throw new IllegalArgumentException("이미 등록된 이미지 URL입니다: " + imageUrl);
        }
    }
}
