package com.nextdoor.nextdoor.domain.rentalreservation.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Builder
    public AiImage(RentalReservation rental, AiImageType type, String imageUrl, String mimeType) {
        this.rental = rental;
        this.type = type;
        this.imageUrl = imageUrl;
        this.mimeType = mimeType;
    }
}
