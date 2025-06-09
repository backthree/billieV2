package com.nextdoor.nextdoor.domain.chat.controller;

import com.nextdoor.nextdoor.domain.chat.domain.ChatMessage;
import com.nextdoor.nextdoor.domain.chat.domain.ChatRoom;
import com.nextdoor.nextdoor.domain.chat.dto.*;
import com.nextdoor.nextdoor.domain.chat.port.ChatMemberQueryPort;
import com.nextdoor.nextdoor.domain.chat.port.ChatPostQueryPort;
import com.nextdoor.nextdoor.domain.chat.port.ChatRentalQueryPort;
import com.nextdoor.nextdoor.domain.chat.port.ChatReservationQueryPort;
import com.nextdoor.nextdoor.domain.chat.service.ChatRoomService;
import com.nextdoor.nextdoor.domain.chat.service.MessageService;
import com.nextdoor.nextdoor.domain.chat.service.UnreadCounterService;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationProcess;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * REST API 컨트롤러: 채팅방 목록/생성/삭제, 메시지 조회/전송/회수/검색
 */
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatRestController {
    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final UnreadCounterService unreadCounterService;
    private final ChatMemberQueryPort chatMemberQueryPort;
    private final ChatPostQueryPort chatPostQueryPort;
    private final ChatReservationQueryPort chatReservationQueryPort;
    private final ChatRentalQueryPort chatRentalQueryPort;

    /** 1) 채팅방 목록 조회 */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> listRooms(@AuthenticationPrincipal Long userId) {
        List<ChatRoom> rooms = chatRoomService.findRoomsByUser(userId);
        List<ChatRoomDto> dtos = rooms.stream().map(room -> {
            // 마지막 메시지 & 시각
            String lastMessage = room.getMessages().isEmpty()
                    ? ""
                    : room.getMessages().get(room.getMessages().size() - 1).getContent();
            LocalDateTime lastSentAt = room.getMessages().isEmpty()
                    ? room.getCreatedAt()
                    : room.getMessages().get(room.getMessages().size() - 1).getSentAt();
            // 미확인 메시지 개수
            long unreadCount = unreadCounterService.getCount(room.getId(), userId);
            // 상대 사용자 ID(오너/렌터 반대측)
            Long otherId = room.getOwnerId().equals(userId)
                    ? room.getRenterId()
                    : room.getOwnerId();
            // 상대 사용자 정보
            MemberDto other = chatMemberQueryPort.findById(otherId)
                    .orElse(MemberDto.builder().nickname("").profileImageUrl("").build());
            // 게시물 정보
            PostDto post = chatPostQueryPort.findById(room.getPostId())
                    .orElse(PostDto.builder().imageUrl("").title("")
                            .rentalFee(0L).deposit(0L).build());
            // 채팅방 상태 결정
            String chatStatus = determineChatStatus(
                    room.getPostId(), room.getOwnerId(), room.getRenterId()
            );
            return ChatRoomDto.builder()
                    .roomId(room.getId())
                    .postId(room.getPostId())
                    .ownerId(room.getOwnerId())
                    .renterId(room.getRenterId())
                    .lastMessage(lastMessage)
                    .lastSentAt(lastSentAt)
                    .unreadCount(unreadCount)
                    .otherNickname(other.getNickname())
                    .otherProfileImageUrl(other.getProfileImageUrl())
                    .postImageUrl(post.getImageUrl())
                    .title(post.getTitle())
                    .rentalFee(post.getRentalFee())
                    .deposit(post.getDeposit())
                    .chatStatus(chatStatus)
                    .build();
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /** 채팅 상태 (예약중, 거래중, 상태없음) 결정 로직 예시 */
    private String determineChatStatus(Long postId, Long ownerId, Long renterId) {
        // 예약 정보 조회
        ReservationDto reservation = chatReservationQueryPort.findByPostIdAndOwnerIdAndRenterId(postId, ownerId, renterId)
                .orElse(null);

        // 예약 정보가 없으면 "상태없음"
        if (reservation == null) {
            return "상태없음";
        }

        // 예약 상태가 PENDING이면 "예약중"
        if (reservation.getStatus() == RentalReservationStatus.PENDING) {
            return "예약중";
        }

        // 렌탈 정보 조회
        Long rentalId = reservation.getRentalId();
        if (rentalId == null) {
            return "상태없음";
        }

        RentalDto rental = chatRentalQueryPort.findById(rentalId).orElse(null);
        if (rental == null) {
            return "상태없음";
        }

        // 렌탈 프로세스가 RENTAL_COMPLETED가 아니면 "거래중"
        if (rental.getRentalReservationProcess() != RentalReservationProcess.RENTAL_COMPLETED) {
            return "거래중";
        }

        // 그 외에는 "상태없음"
        return "상태없음";
    }

    /** 2) 채팅방 생성 */
    @PostMapping("/rooms")
    public ResponseEntity<Map<String, Long>> createRoom(
            @RequestBody CreateRoomRequest request) {
        ChatRoom room = chatRoomService.createRoom(
                request.getPostId(),
                request.getOwnerId(),
                request.getRenterId()
        );
        return ResponseEntity.ok(Collections.singletonMap("roomId", room.getId()));
    }
    /** 3) 채팅 메시지 이력 조회 */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<MessageDto>> getHistory(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<ChatMessage> histories = messageService.getHistory(roomId, userId, page, size);
        Page<MessageDto> dtoPage = histories.map(MessageDto::from);
        return ResponseEntity.ok(dtoPage);
    }

    /** 4) 채팅 메시지 전송 */
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<MessageDto> sendMessage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId,
            @RequestBody SendMessageRequest request) {
        ChatMessage saved = messageService.sendMessage(roomId, userId, request.getContent());
        return ResponseEntity.ok(MessageDto.from(saved));
    }

    /** 5) 채팅 메시지 삭제(회수) */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long messageId) {
        messageService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    /** 6) 키워드 검색 */
    @GetMapping("/rooms/{roomId}/messages/search")
    public ResponseEntity<Page<MessageDto>> searchMessages(
            @PathVariable Long roomId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<ChatMessage> results = messageService.searchMessages(roomId, keyword, page, size);
        Page<MessageDto> dtoPage = results.map(MessageDto::from);
        return ResponseEntity.ok(dtoPage);
    }

    /** 7) 채팅방 삭제 */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId) {
        chatRoomService.deleteRoom(roomId, userId);
        return ResponseEntity.noContent().build();
    }

    /** 채팅방 생성용 요청 DTO */
    @Data
    public static class CreateRoomRequest {
        private Long postId;
        private Long ownerId;
        private Long renterId;
    }
}
