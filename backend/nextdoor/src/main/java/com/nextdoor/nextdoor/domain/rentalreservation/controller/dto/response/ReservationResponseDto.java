package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.response;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.PostDto;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.ReservationMemberQueryDto;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.ReservationQueryDto;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class ReservationResponseDto {

    private Long reservationId;
    private String postTitle;
    private String postProductImage;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal rentalFee;
    private BigDecimal deposit;
    private String status;
    private String ownerName;
    private String ownerProfileImageUrl;
    private String renterName;
    private String renterProfileImageUrl;

    public static ReservationResponseDto from(RentalReservation rentalReservation, PostDto post, ReservationMemberQueryDto renter) {
        ReservationResponseDto response = new ReservationResponseDto();
        response.reservationId = rentalReservation.getId();
        response.postTitle = post.getTitle();
        response.postProductImage = post.getProductImage();
        response.startDate = rentalReservation.getStartDate();
        response.endDate = rentalReservation.getEndDate();
        response.rentalFee = rentalReservation.getRentalFee();
        response.deposit = rentalReservation.getDeposit();
        response.status = rentalReservation.getRentalReservationStatus().name();
        response.ownerName = post.getAuthorName();
        response.ownerProfileImageUrl = post.getAuthorProfileImageUrl();
        response.renterName = renter.getNickname();
        response.renterProfileImageUrl = renter.getProfileImageUrl();
        return response;
    }

    public static ReservationResponseDto from(ReservationQueryDto reservation) {
        ReservationResponseDto response = new ReservationResponseDto();
        response.reservationId = reservation.getReservationId();
        response.postTitle = reservation.getPostTitle();
        response.postProductImage = reservation.getPostProductImage();
        response.startDate = reservation.getStartDate();
        response.endDate = reservation.getEndDate();
        response.rentalFee = reservation.getRentalFee();
        response.deposit = reservation.getDeposit();
        response.status = reservation.getStatus();
        response.ownerName = reservation.getOwnerName();
        response.ownerProfileImageUrl = reservation.getOwnerProfileImageUrl();
        response.renterName = reservation.getRenterName();
        response.renterProfileImageUrl = reservation.getRenterProfileImageUrl();
        return response;
    }
}