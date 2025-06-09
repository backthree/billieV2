package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.member.domain.QMember;
import com.nextdoor.nextdoor.domain.post.domain.QPost;
import com.nextdoor.nextdoor.domain.post.domain.QProductImage;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.QRentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.NoSuchRentalException;
import com.nextdoor.nextdoor.domain.rentalreservation.exception.NoSuchReservationException;
import com.nextdoor.nextdoor.domain.rentalreservation.message.RentalStatusMessage;
import com.nextdoor.nextdoor.domain.rentalreservation.port.RentalDetailQueryPort;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Adapter
@RequiredArgsConstructor
public class RentalDetailQueryAdapter implements RentalDetailQueryPort {

    private final JPAQueryFactory queryFactory;
    private final QRentalReservation rentalReservation = QRentalReservation.rentalReservation;
    private final QPost post = QPost.post;
    private final QProductImage productImage = QProductImage.productImage;
    private final QMember member = QMember.member;

    public RentalStatusMessage.RentalDetailResult getRentalDetailByRentalIdAndRole(Long rentalId) {
        RentalReservation rental = queryFactory
                .selectFrom(rentalReservation)
                .where(rentalReservation.id.eq(rentalId))
                .fetchOne();

        if (rental == null) {
            throw new NoSuchRentalException("대여 정보가 존재하지 않습니다. rentalId: " + rentalId);
        }

        Long postId = rental.getPostId();

        if (postId == null) {
            throw new NoSuchReservationException("예약 정보가 존재하지 않습니다. rentalId: " + rentalId);
        }

        String postTitle = queryFactory
                .select(post.title)
                .from(post)
                .where(post.id.eq(postId))
                .fetchOne();

        String representativeImageUrl = queryFactory
                .select(productImage.imageUrl)
                .from(productImage)
                .where(productImage.post.id.eq(postId))
                .fetchFirst();


        Long renterId = queryFactory
                .select(rentalReservation.renterId)
                .from(rentalReservation)
                .where(rentalReservation.id.eq(rentalId))
                .fetchOne();

        Long ownerId = queryFactory
                .select(rentalReservation.ownerId)
                .from(rentalReservation)
                .where(rentalReservation.id.eq(rentalId))
                .fetchOne();


        String renterProfileImageUrl = queryFactory
                .select(member.profileImageUrl)
                .from(member)
                .where(member.id.eq(renterId))
                .fetchOne();

        String ownerProfileImageUrl = queryFactory
                .select(member.profileImageUrl)
                .from(member)
                .where(member.id.eq(ownerId))
                .fetchOne();

        return RentalStatusMessage.RentalDetailResult.builder()
                .postTitle(postTitle)
                .representativeImageUrl(representativeImageUrl)
                .renterProfileImageUrl(renterProfileImageUrl)
                .ownerProfileImageUrl(ownerProfileImageUrl)
                .build();
    }
}