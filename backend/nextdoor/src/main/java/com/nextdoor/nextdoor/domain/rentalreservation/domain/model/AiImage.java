package com.nextdoor.nextdoor.domain.rentalreservation.domain.model;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.exception.RentalImageUploadException;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_image")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AiImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id")
    private RentalReservation rental;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AiImageType type;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Builder(access = AccessLevel.PRIVATE)
    private AiImage(RentalReservation rental, AiImageType type, String imageUrl, String mimeType) {
        this.rental = rental;
        this.type = type;
        this.imageUrl = imageUrl;
        this.mimeType = mimeType;
    }

    public static AiImage create(RentalReservation rental, AiImageType type, String imageUrl, String mimeType){
        validateNotBlank(imageUrl, "imageUrl");
        validateNotBlank(mimeType, "mimeType");

        if(rental==null){
            throw new IllegalArgumentException("RentalReservation은 필수 값입니다.");
        }

        if(type==null){
            throw new IllegalArgumentException("이미지 타입은 필수 값입니다.");
        }

        return AiImage.builder()
                .rental(rental)
                .type(type)
                .imageUrl(imageUrl)
                .mimeType(mimeType)
                .build();
    }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수 값입니다.");
        }
    }
}
