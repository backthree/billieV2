package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.member.domain.QMember;
import com.nextdoor.nextdoor.domain.post.domain.QPost;
import com.nextdoor.nextdoor.domain.post.domain.QProductImage;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.QRentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationProcess;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationStatus;
import com.nextdoor.nextdoor.domain.rentalreservation.port.RentalQueryPort;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.ManagedRentalCountResult;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.RequestRemittanceResult;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.SearchRentalCommand;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.SearchRentalResult;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Adapter
@RequiredArgsConstructor
public class RentalQueryAdapter implements RentalQueryPort {

    private final JPAQueryFactory queryFactory;
    private final QRentalReservation rentalReservation = QRentalReservation.rentalReservation;
    private final QPost post = QPost.post;
    private final QMember member = QMember.member;
    private final QMember owner = new QMember("owner");
    private final QMember renter = new QMember("renter");
    private final QProductImage productImage = QProductImage.productImage;

    @Override
    public Page<SearchRentalResult> searchRentals(SearchRentalCommand command) {
        Long userId = command.getUserId();
        String userRole = command.getUserRole();
        String condition = command.getCondition();
        Pageable pageable = command.getPageable();

        BooleanBuilder whereCondition = new BooleanBuilder();

        if(userRole.equalsIgnoreCase("OWNER")){
            whereCondition.and(rentalReservation.ownerId.eq(userId));
        }else if(userRole.equalsIgnoreCase("RENTER")){
            whereCondition.and(rentalReservation.renterId.eq(userId));
        }

        whereCondition.and(rentalProcessCondition(condition));

        List<SearchRentalResult> results = queryFactory
                .select(Projections.fields(SearchRentalResult.class,
                        rentalReservation.id.as("id"),
                        rentalReservation.postId,
                        rentalReservation.startDate.as("startDate"),
                        rentalReservation.endDate.as("endDate"),
                        rentalReservation.rentalFee,
                        rentalReservation.deposit,
                        rentalReservation.ownerId,
                        owner.profileImageUrl.as("ownerProfileImageUrl"),
                        rentalReservation.renterId,
                        renter.profileImageUrl.as("renterProfileImageUrl"),
                        rentalReservation.id.as("rentalId"),
                        rentalReservation.rentalReservationProcess.stringValue().as("rentalProcess"),
                        rentalReservation.rentalReservationStatus.stringValue().as("rentalStatus"),
                        post.title,
                        rentalReservation.createdAt))
                .from(rentalReservation)
                .join(post).on(rentalReservation.postId.eq(post.id))
                .join(owner).on(rentalReservation.ownerId.eq(owner.id))
                .join(renter).on(rentalReservation.renterId.eq(renter.id))
                .where(whereCondition)
                .distinct()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifiers(pageable))
                .fetch();

        for (SearchRentalResult result : results) {
            List<String> imageUrls = queryFactory
                    .select(productImage.imageUrl)
                    .from(productImage)
                    .join(productImage.post, post)
                    .join(rentalReservation).on(rentalReservation.postId.eq(post.id))
                    .where(rentalReservation.id.eq(result.getId()))
                    .fetch();

            result.setProductImages(imageUrls);
        }

        Long total = queryFactory
                .select(rentalReservation.count())
                .from(rentalReservation)
                .join(post).on(rentalReservation.postId.eq(post.id))
                .where(whereCondition)
                .fetchOne();

        return new PageImpl<>(results, pageable, total != null ? total : 0L);
    }

    @Override
    public Optional<RequestRemittanceResult> findRemittanceRequestViewData(Long rentalId) {
        return Optional.ofNullable(
                queryFactory
                        .select(Projections.constructor(
                                RequestRemittanceResult.class,
                                member.nickname.as("ownerNickname"),
                                member.profileImageUrl.as("ownerProfileImageUrl"),
                                rentalReservation.finalAmount,
                                rentalReservation.deposit,
                                rentalReservation.accountNo,
                                rentalReservation.bankCode,
                                rentalReservation.startDate,
                                rentalReservation.endDate
                        ))
                        .from(rentalReservation)
                        .join(member).on(rentalReservation.ownerId.eq(member.id))
                        .where(rentalReservation.id.eq(rentalId))
                        .fetchOne()
        );
    }


    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        if (!pageable.getSort().isEmpty()) {
            List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

            for (Sort.Order order : pageable.getSort()) {
                Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;

                switch (order.getProperty()) {
                    case "createdAt":
                        orderSpecifiers.add(new OrderSpecifier<>(direction, rentalReservation.createdAt));
                        break;

                    default:
                        orderSpecifiers.add(new OrderSpecifier<>(direction, rentalReservation.createdAt));
                        break;
                }
            }

            return orderSpecifiers.toArray(new OrderSpecifier[0]);
        }

        return new OrderSpecifier[] {new OrderSpecifier<>(Order.DESC, rentalReservation.createdAt)};
    }

    private BooleanExpression rentalProcessCondition(String condition){
        if(!StringUtils.hasText(condition)){
            return null;
        }

        if (condition.contains("ACTIVE")) {
            return rentalReservation.rentalReservationProcess.ne(RentalReservationProcess.RENTAL_COMPLETED)
                    .and(rentalReservation.rentalReservationStatus.ne(RentalReservationStatus.PENDING));
        } else if (condition.contains("COMPLETED")) {
            return rentalReservation.rentalReservationProcess.eq(RentalReservationProcess.RENTAL_COMPLETED);
        }

        return null;
    }

    @Override
    public ManagedRentalCountResult countManagedRentals(Long ownerId) {
        Long count = queryFactory
                .select(rentalReservation.count())
                .from(rentalReservation)
                .where(
                    rentalReservation.ownerId.eq(ownerId),
                    rentalReservation.rentalReservationProcess.ne(RentalReservationProcess.RENTAL_COMPLETED)
                )
                .fetchOne();

        return ManagedRentalCountResult.builder()
                .managedRentalCount(count != null ? count : 0L)
                .build();
    }

    @Override
    public Optional<SearchRentalResult> findRentalById(Long rentalId) {
        SearchRentalResult result = queryFactory
                .select(Projections.fields(SearchRentalResult.class,
                        rentalReservation.id.as("id"),
                        rentalReservation.postId,
                        rentalReservation.startDate.as("startDate"),
                        rentalReservation.endDate.as("endDate"),
                        rentalReservation.rentalFee,
                        rentalReservation.deposit,
                        rentalReservation.ownerId,
                        rentalReservation.renterId,
                        rentalReservation.id.as("rentalId"),
                        rentalReservation.rentalReservationProcess.stringValue().as("rentalProcess"),
                        rentalReservation.rentalReservationStatus.stringValue().as("rentalStatus"),
                        post.title,
                        rentalReservation.createdAt))
                .from(rentalReservation)
                .join(post).on(rentalReservation.postId.eq(post.id))
                .where(rentalReservation.id.eq(rentalId))
                .fetchOne();

        if (result != null) {
            List<String> imageUrls = queryFactory
                    .select(productImage.imageUrl)
                    .from(productImage)
                    .join(productImage.post, post)
                    .join(rentalReservation).on(rentalReservation.postId.eq(post.id))
                    .where(rentalReservation.id.eq(result.getId()))
                    .fetch();

            result.setProductImages(imageUrls);
        }

        return Optional.ofNullable(result);
    }
}