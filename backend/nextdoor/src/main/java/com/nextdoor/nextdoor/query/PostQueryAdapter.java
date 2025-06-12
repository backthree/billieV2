package com.nextdoor.nextdoor.query;

import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.member.domain.model.QMember;
import com.nextdoor.nextdoor.domain.post.domain.Post;
import com.nextdoor.nextdoor.domain.post.domain.PostLikeCount;
import com.nextdoor.nextdoor.domain.post.domain.QPost;
import com.nextdoor.nextdoor.domain.post.domain.QPostLike;
import com.nextdoor.nextdoor.domain.post.domain.QPostLikeCount;
import com.nextdoor.nextdoor.domain.post.domain.QProductImage;
import com.nextdoor.nextdoor.domain.post.exception.NoSuchPostException;
import com.nextdoor.nextdoor.domain.post.port.PostQueryPort;
import com.nextdoor.nextdoor.domain.post.service.dto.PostDetailResult;
import com.nextdoor.nextdoor.domain.post.service.dto.LocationDto;
import com.nextdoor.nextdoor.domain.post.service.dto.SearchPostCommand;
import com.nextdoor.nextdoor.domain.post.service.dto.SearchPostResult;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@Adapter
@RequiredArgsConstructor
public class PostQueryAdapter implements PostQueryPort {

    private final JPAQueryFactory queryFactory;
    private final QPost post = QPost.post;
    private final QMember member = QMember.member;
    private final QProductImage productImage = QProductImage.productImage;
    private final QPostLike postLike = QPostLike.postLike;
    private final QPostLikeCount postLikeCount = QPostLikeCount.postLikeCount;

    @Override
    public Page<SearchPostResult> searchPostsByMemberAddress(SearchPostCommand command) {
        Long userId = command.getUserId();
        String userAddress = queryFactory
                .select(member.address)
                .from(member)
                .where(member.id.eq(userId))
                .fetchOne();

        JPAQuery<SearchPostResult> query = queryFactory
                .select(Projections.constructor(
                        SearchPostResult.class,
                        post.id,
                        post.title,
                        queryFactory
                                .select(productImage.imageUrl.min())
                                .from(productImage)
                                .where(productImage.post.id.eq(post.id)),
                        post.rentalFee,
                        post.deposit,
                        postLikeCount.likeCount.intValue().coalesce(0),
                        Expressions.constant(0)
                ))
                .from(post)
                .join(member).on(post.authorId.eq(member.id))
                .leftJoin(postLikeCount).on(postLikeCount.postId.eq(post.id))
                .groupBy(post.id);

        if (userAddress != null) {
            query = query.where(post.address.eq(userAddress));
        }

        long total = query.fetchCount();

        Pageable pageable = command.getPageable();
        List<SearchPostResult> results = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifier(command.getPageable().getSort()))
                .fetch();

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public Page<SearchPostResult> searchPostsLikedByMember(SearchPostCommand command) {
        Long memberId = command.getUserId();

        JPAQuery<SearchPostResult> query = queryFactory
                .select(Projections.constructor(
                        SearchPostResult.class,
                        post.id,
                        post.title,
                        queryFactory
                                .select(productImage.imageUrl.min())
                                .from(productImage)
                                .where(productImage.post.id.eq(post.id)),
                        post.rentalFee,
                        post.deposit,
                        postLikeCount.likeCount.intValue().coalesce(0),
                        Expressions.constant(0)
                ))
                .from(post)
                .join(postLike).on(postLike.post.eq(post).and(postLike.memberId.eq(memberId)))
                .leftJoin(postLikeCount).on(postLikeCount.postId.eq(post.id))
                .groupBy(post.id);

        long total = query.fetchCount();

        Pageable pageable = command.getPageable();
        List<SearchPostResult> results = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(results, pageable, total);
    }

    public PostDetailResult getPostDetail(Long postId) {
        Post postEntity = queryFactory
                .selectFrom(post)
                .where(post.id.eq(postId))
                .fetchOne();

        if(postEntity == null){
            throw new NoSuchPostException("ID가 " + postId + "인 게시물이 존재하지 않습니다.");
        }

        String nickname = queryFactory
                .select(member.nickname)
                .from(member)
                .where(member.id.eq(postEntity.getAuthorId()))
                .fetchOne();

        List<String> productImages = queryFactory
                .select(productImage.imageUrl)
                .from(productImage)
                .where(productImage.post.id.eq(postId))
                .fetch();

        LocationDto locationDto = null;
        if (postEntity.getLatitude() != null && postEntity.getLongitude() != null) {
            locationDto = new LocationDto(postEntity.getLatitude(), postEntity.getLongitude());
        }

        Integer likeCount = queryFactory
                .select(postLikeCount.likeCount.intValue())
                .from(postLikeCount)
                .where(postLikeCount.postId.eq(postId))
                .fetchOne();

        return PostDetailResult.builder()
                .title(postEntity.getTitle())
                .content(postEntity.getContent())
                .rentalFee(Math.toIntExact(postEntity.getRentalFee()))
                .deposit(Math.toIntExact(postEntity.getDeposit()))
                .address(postEntity.getAddress())
                .location(locationDto)
                .productImages(productImages)
                .category(postEntity.getCategory().toString())
                .authorId(postEntity.getAuthorId())
                .nickname(nickname)
                .likeCount(likeCount != null ? likeCount : 0)
                .build();
    }

    private OrderSpecifier<?>[] getOrderSpecifier(Sort sort) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        sort.stream().forEach(order -> {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String property = order.getProperty();

            PathBuilder<Post> pathBuilder = new PathBuilder<>(Post.class, "post");
            orderSpecifiers.add(new OrderSpecifier(direction, pathBuilder.get(property)));
        });

        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }
}
