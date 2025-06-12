package com.nextdoor.nextdoor.domain.fintech.service;

import com.nextdoor.nextdoor.domain.auth.port.AuthFintechCommandPort;
import com.nextdoor.nextdoor.domain.fintech.client.SsafyApiClient;
import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import com.nextdoor.nextdoor.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FintechUserService implements AuthFintechCommandPort {
    private final SsafyApiClient client;
    private final MemberRepository memberRepository;
    private final AccountService accountService;

    /**
     * 1) SSAFY API 로 user 생성 → userKey 발급
     * 2) 기존 Member 조회 후 userKey 업데이트
     * 3) 빌리페이 생성
     */

    // 계정 생성 (빌리페이 생성도 같이함)
    @Override
    public Mono<Map<String,Object>> createUser(Long memberId, String ssafyApiEmail) {
        return client.createUser(memberId, ssafyApiEmail)
                .flatMap(ssafyResp -> {
                    String userKey = ssafyResp.get("userKey").toString();

                    // 2) 블로킹으로 기존 Member 조회 & userKey 업데이트
                    return Mono.fromCallable(() -> {
                                Member member = memberRepository.findById(memberId)
                                        .orElseThrow(() -> new RuntimeException("Member가 없습니다: " + memberId));
                                member.updateUserKey(userKey);
                                return memberRepository.save(member);
                            })
                            .subscribeOn(Schedulers.boundedElastic())

                            // 3) 저장이 끝나면 빌리페이 생성 → 그리고 원래 SSAFY 응답 Map 리턴
                            .flatMap(updated -> accountService.createBilipay(userKey))
                            .thenReturn(ssafyResp);
                });
    }
}
