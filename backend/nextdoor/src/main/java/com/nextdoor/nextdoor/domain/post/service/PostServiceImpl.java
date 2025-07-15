package com.nextdoor.nextdoor.domain.post.service;

import com.nextdoor.nextdoor.domain.aianalysis.controller.dto.response.ProductConditionAnalysisResponseDto;
import com.nextdoor.nextdoor.domain.post.controller.dto.response.AnalyzeProductImageResponse;
import com.nextdoor.nextdoor.domain.post.controller.dto.response.CombinedProductAnalysisResponse;
import com.nextdoor.nextdoor.domain.post.domain.Post;
import com.nextdoor.nextdoor.domain.post.domain.PostLikeCount;
import com.nextdoor.nextdoor.domain.post.event.PostCreatedEvent;
import com.nextdoor.nextdoor.domain.post.event.PostUpdatedEvent;
import com.nextdoor.nextdoor.domain.post.exception.NoSuchPostException;
import com.nextdoor.nextdoor.domain.post.exception.PostImageUploadException;
import com.nextdoor.nextdoor.domain.post.mapper.PostMapper;
import com.nextdoor.nextdoor.domain.post.port.*;
import com.nextdoor.nextdoor.domain.post.repository.PostLikeCountRepository;
import com.nextdoor.nextdoor.domain.post.repository.PostLikeRepository;
import com.nextdoor.nextdoor.domain.post.repository.PostRepository;
import com.nextdoor.nextdoor.domain.post.service.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostQueryPort postQueryPort;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostLikeCountRepository postLikeCountRepository;
    private final S3ImageUploadPort s3ImageUploadPort;
    private final PostMapper postMapper;
    private final ProductAnalysisPort productAnalysisPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<SearchPostResult> searchPostsByUserAddress(SearchPostCommand command) {
        return postQueryPort.searchPostsByMemberAddress(command);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDetailResult getPostDetail(PostDetailCommand command) {
        return postQueryPort.getPostDetail(command.getPostId());
    }

    @Override
    @Transactional
    public CreatePostResult createPost(CreatePostCommand command) {
        Double latitude = null;
        Double longitude = null;
        if (command.getPreferredLocation() != null) {
            latitude = command.getPreferredLocation().getLatitude();
            longitude = command.getPreferredLocation().getLongitude();
        }

        Post post = Post.builder()
                .title(command.getTitle())
                .content(command.getContent())
                .rentalFee(command.getRentalFee())
                .deposit(command.getDeposit())
                .address(command.getAddress())
                .latitude(latitude)
                .longitude(longitude)
                .category(command.getCategory())
                .authorId(command.getAuthorId())
                .productImages(new ArrayList<>())
                .build();

        Post savedPost = postRepository.save(post);

        eventPublisher.publishEvent(PostCreatedEvent.builder()
                .postId(savedPost.getId())
                .build());

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile image : command.getProductImages()) {
            try {
                String imageUrl = s3ImageUploadPort.uploadProductImage(image, savedPost.getId());
                imageUrls.add(imageUrl);
                savedPost.addProductImage(imageUrl);
            } catch (Exception e) {
                throw new PostImageUploadException("게시물 이미지 업로드에 실패했습니다.", e);
            }
        }

        return postMapper.toCreateResult(savedPost, imageUrls);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzeProductImageResponse analyzeProductImage(MultipartFile productImage) {
        return productAnalysisPort.analyzeProductImage(productImage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductConditionAnalysisResponseDto analyzeProductCondition(MultipartFile productImage) {
        return productAnalysisPort.analyzeProductCondition(productImage);
    }

    @Override
    @Transactional(readOnly = true)
    public CombinedProductAnalysisResponse analyzeProduct(MultipartFile productImage) {
        AnalyzeProductImageResponse imageResponse = analyzeProductImage(productImage);
        ProductConditionAnalysisResponseDto conditionResponse = analyzeProductCondition(productImage);

        return CombinedProductAnalysisResponse.from(imageResponse, conditionResponse);
    }

    @Override
    @Transactional
    public boolean likePost(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchPostException("ID가 " + postId + "인 게시물이 존재하지 않습니다."));

        if (postLikeRepository.existsByPostAndMemberId(post, memberId)) {
            return false;
        }

        post.addLike(memberId);

        postLikeCountRepository.findById(postId)
                .orElseGet(() -> new PostLikeCount(postId, 0L));

        postLikeCountRepository.incrementLikeCount(postId);

        return true;
    }

    @Override
    @Transactional
    public boolean unlikePost(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchPostException("ID가 " + postId + "인 게시물이 존재하지 않습니다."));

        if (!postLikeRepository.existsByPostAndMemberId(post, memberId)) {
            return false;
        }

        post.removeLike(memberId);

        postLikeCountRepository.findById(postId)
                .orElseGet(() -> new PostLikeCount(postId, 0L));

        postLikeCountRepository.decrementLikeCount(postId);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPostLikedByMember(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchPostException("ID가 " + postId + "인 게시물이 존재하지 않습니다."));

        return postLikeRepository.existsByPostAndMemberId(post, memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getPostLikeCount(Long postId) {
        return postLikeCountRepository.findById(postId)
                .map(PostLikeCount::getLikeCount)
                .map(Long::intValue)
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SearchPostResult> getLikedPostsByMember(SearchPostCommand command) {
        return postQueryPort.searchPostsLikedByMember(command);
    }

    @Override
    @Transactional
    public UpdatePostResult updatePost(UpdatePostCommand command) {
        Post post = postRepository.findById(command.getPostId())
                .orElseThrow(() -> new NoSuchPostException("ID가 " + command.getPostId() + "인 게시물이 존재하지 않습니다."));

        if (!post.getAuthorId().equals(command.getAuthorId())) {
            throw new IllegalArgumentException("게시물 작성자만 수정할 수 있습니다.");
        }

        Post updatedPost = Post.builder()
                .id(post.getId())
                .title(command.getTitle() != null ? command.getTitle() : post.getTitle())
                .content(command.getContent() != null ? command.getContent() : post.getContent())
                .category(command.getCategory() != null ? command.getCategory() : post.getCategory())
                .rentalFee(command.getRentalFee() != null ? command.getRentalFee() : post.getRentalFee())
                .deposit(command.getDeposit() != null ? command.getDeposit() : post.getDeposit())
                .address(command.getAddress() != null ? command.getAddress() : post.getAddress())
                .latitude(command.getPreferredLocation() != null ? command.getPreferredLocation().getLatitude() : post.getLatitude())
                .longitude(command.getPreferredLocation() != null ? command.getPreferredLocation().getLongitude() : post.getLongitude())
                .authorId(post.getAuthorId())
                .productImages(new ArrayList<>(post.getProductImages()))
                .build();

        updatedPost = postRepository.save(updatedPost);

        eventPublisher.publishEvent(PostUpdatedEvent.builder()
                .postId(updatedPost.getId())
                .build());

        List<String> imageUrls = new ArrayList<>();
        if (command.getProductImages() != null && !command.getProductImages().isEmpty()) {
            updatedPost = Post.builder()
                    .id(updatedPost.getId())
                    .title(updatedPost.getTitle())
                    .content(updatedPost.getContent())
                    .category(updatedPost.getCategory())
                    .rentalFee(updatedPost.getRentalFee())
                    .deposit(updatedPost.getDeposit())
                    .address(updatedPost.getAddress())
                    .latitude(updatedPost.getLatitude())
                    .longitude(updatedPost.getLongitude())
                    .authorId(updatedPost.getAuthorId())
                    .productImages(new ArrayList<>())
                    .build();

            updatedPost = postRepository.save(updatedPost);

            for (MultipartFile image : command.getProductImages()) {
                try {
                    String imageUrl = s3ImageUploadPort.uploadProductImage(image, updatedPost.getId());
                    imageUrls.add(imageUrl);
                    updatedPost.addProductImage(imageUrl);
                } catch (Exception e) {
                    throw new PostImageUploadException("게시물 이미지 업로드에 실패했습니다.", e);
                }
            }
        } else {
            updatedPost.getProductImages().forEach(image -> imageUrls.add(image.getImageUrl()));
        }

        return postMapper.toUpdateResult(updatedPost, imageUrls);
    }

    @Override
    @Transactional
    public boolean deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchPostException("ID가 " + postId + "인 게시물이 존재하지 않습니다."));

        if (!post.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("게시물 작성자만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
        postLikeCountRepository.findById(postId).ifPresent(postLikeCountRepository::delete);

        eventPublisher.publishEvent(PostUpdatedEvent.builder()
                .postId(postId)
                .build());

        return true;
    }
}
