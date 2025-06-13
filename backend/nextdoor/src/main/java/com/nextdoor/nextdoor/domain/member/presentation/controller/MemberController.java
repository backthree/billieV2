package com.nextdoor.nextdoor.domain.member.presentation.controller;

import com.nextdoor.nextdoor.domain.member.presentation.dto.request.MemberExtraInfoSaveRequestDto;
import com.nextdoor.nextdoor.domain.member.presentation.dto.response.MemberResponseDto;
import com.nextdoor.nextdoor.domain.member.application.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @PutMapping
    public ResponseEntity<MemberResponseDto> updateMember(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MemberExtraInfoSaveRequestDto member) {
        return ResponseEntity.ok(memberService.updateMember(memberId, member));
    }

    @GetMapping
    public ResponseEntity<MemberResponseDto> retrieveMember(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(memberService.retrieveMember(memberId));
    }
}
