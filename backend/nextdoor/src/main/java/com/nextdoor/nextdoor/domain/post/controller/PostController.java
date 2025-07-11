package com.nextdoor.nextdoor.domain.post.controller;

import com.nextdoor.nextdoor.domain.aianalysis.controller.dto.response.ProductConditionAnalysisResponseDto;
import com.nextdoor.nextdoor.domain.auth.jwt.JwtProvider;
import com.nextdoor.nextdoor.domain.post.controller.dto.request.CreatePostRequest;
import com.nextdoor.nextdoor.domain.post.controller.dto.request.UpdatePostRequest;
import com.nextdoor.nextdoor.domain.post.controller.dto.response.AnalyzeProductImageResponse;
import com.nextdoor.nextdoor.domain.post.controller.dto.response.CombinedProductAnalysisResponse;
import com.nextdoor.nextdoor.domain.post.controller.dto.response.CreatePostResponse;
import com.nextdoor.nextdoor.domain.post.controller.dto.response.PostDetailResponse;
import com.nextdoor.nextdoor.domain.post.controller.dto.response.PostLikeResponse;
import com.nextdoor.nextdoor.domain.post.controller.dto.response.PostListResponse;
import com.nextdoor.nextdoor.domain.post.controller.dto.response.TokensResponse;
import com.nextdoor.nextdoor.domain.post.controller.dto.response.UpdatePostResponse;
import com.nextdoor.nextdoor.domain.post.mapper.PostMapper;
import com.nextdoor.nextdoor.domain.post.service.PostService;
import com.nextdoor.nextdoor.domain.post.service.dto.CreatePostCommand;
import com.nextdoor.nextdoor.domain.post.service.dto.CreatePostResult;
import com.nextdoor.nextdoor.domain.post.service.dto.PostDetailCommand;
import com.nextdoor.nextdoor.domain.post.service.dto.PostDetailResult;
import com.nextdoor.nextdoor.domain.post.service.dto.SearchPostCommand;
import com.nextdoor.nextdoor.domain.post.service.dto.SearchPostResult;
import com.nextdoor.nextdoor.domain.post.service.dto.UpdatePostCommand;
import com.nextdoor.nextdoor.domain.post.service.dto.UpdatePostResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostMapper postMapper;

    @GetMapping
    public ResponseEntity<Page<PostListResponse>> getPostsByUserAddress(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        SearchPostCommand command = postMapper.toCommand(userId, pageable);
        Page<SearchPostResult> results = postService.searchPostsByUserAddress(command);
        Page<PostListResponse> responsePage = results.map(postMapper::toResponse);

        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {
        PostDetailCommand command = postMapper.toDetailCommand(postId, userId);
        PostDetailResult result = postService.getPostDetail(command);
        boolean isLiked = postService.isPostLikedByMember(postId, userId);
        PostDetailResponse response = postMapper.toDetailResponse(result, isLiked);

        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreatePostResponse> createPost(
            @RequestPart("post") @Valid CreatePostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal Long authorId
    ) {
        CreatePostCommand command = postMapper.toCreateCommand(request, images, authorId);
        CreatePostResult result = postService.createPost(command);
        CreatePostResponse response = postMapper.toCreateResponse(result);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalyzeProductImageResponse> analyzeProductImage(
            @RequestPart("image") MultipartFile image
    ) {
        AnalyzeProductImageResponse response = postService.analyzeProductImage(image);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/analyze-condition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductConditionAnalysisResponseDto> analyzeProductCondition(
            @RequestPart("image") MultipartFile productImage
    ) {
        ProductConditionAnalysisResponseDto response = postService.analyzeProductCondition(productImage);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CombinedProductAnalysisResponse> analyzeProduct(
            @RequestPart("image") MultipartFile productImage
    ) {
        CombinedProductAnalysisResponse response = postService.analyzeProduct(productImage);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<PostLikeResponse> likePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ) {
        boolean success = postService.likePost(postId, userId);
        int likeCount = postService.getPostLikeCount(postId);
        if (success) likeCount++;

        PostLikeResponse response = PostLikeResponse.of(postId, true, likeCount);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<PostLikeResponse> unlikePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ) {
        postService.unlikePost(postId, userId);
        int likeCount = postService.getPostLikeCount(postId);

        PostLikeResponse response = PostLikeResponse.of(postId, false, likeCount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}/like")
    public ResponseEntity<PostLikeResponse> isPostLikedByUser(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ) {
        boolean isLiked = postService.isPostLikedByMember(postId, userId);
        int likeCount = postService.getPostLikeCount(postId);

        PostLikeResponse response = PostLikeResponse.of(postId, isLiked, likeCount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/liked")
    public ResponseEntity<Page<PostListResponse>> getLikedPosts(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        SearchPostCommand command = postMapper.toCommand(userId, pageable);
        Page<SearchPostResult> results = postService.getLikedPostsByMember(command);
        Page<PostListResponse> responsePage = results.map(postMapper::toResponse);

        return ResponseEntity.ok(responsePage);
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UpdatePostResponse> updatePost(
            @PathVariable Long postId,
            @RequestPart("post") @Valid UpdatePostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal Long authorId
    ) {
        UpdatePostCommand command = postMapper.toUpdateCommand(request, images, postId, authorId);
        UpdatePostResult result = postService.updatePost(command);
        UpdatePostResponse response = postMapper.toUpdateResponse(result);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ) {
        boolean success = postService.deletePost(postId, userId);
        if (success) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
