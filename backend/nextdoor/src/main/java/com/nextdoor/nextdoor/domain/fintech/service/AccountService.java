package com.nextdoor.nextdoor.domain.fintech.service;

import com.nextdoor.nextdoor.domain.fintech.client.SsafyApiClient;
import com.nextdoor.nextdoor.domain.fintech.domain.Account;
import com.nextdoor.nextdoor.domain.fintech.domain.RegistAccountType;
import com.nextdoor.nextdoor.domain.fintech.dto.InquireTransactionHistoryRequestDto;
import com.nextdoor.nextdoor.domain.fintech.dto.RegistAccountRequestDto;
import com.nextdoor.nextdoor.domain.fintech.dto.RegistAccountResponseDto;
import com.nextdoor.nextdoor.domain.fintech.event.RemittanceCompletedEvent;
import com.nextdoor.nextdoor.domain.fintech.port.MemberQueryPort;
import com.nextdoor.nextdoor.domain.fintech.repository.AccountRepository;
import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    private final SsafyApiClient client;
    private final AccountRepository accountRepository;
    private final MemberQueryPort memberQueryPort;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    //계좌 생성
    public Mono<Map<String, Object>> createAccount(String userKey, String accountTypeUniqueNo) {
        return client.createAccount(userKey, accountTypeUniqueNo)
                .flatMap(resp -> {
                    // 1) SSAFY 응답에서 실제 생성된 계좌번호 필드명 확인
                    //여기서 발생할 수 있는 unchecked 경고(경고 코드: unchecked)를 무시해 달라”는 지시문
                    @SuppressWarnings("unchecked")
                    Map<String,Object> rec = (Map<String,Object>) resp.get("REC");
                    if (rec == null) {
                        return Mono.error(new RuntimeException("SSAFY 계좌 생성 응답에 REC가 없습니다."));
                    }
                    String acctNo   = rec.get("accountNo").toString();
                    String bankCode = rec.get("bankCode").toString();

                    // 2) Member 조회 & Account 엔티티 생성·저장
                    return Mono.fromCallable(() -> {
                                Member member = memberQueryPort.findByUserKey(userKey)
                                        .orElseThrow(() -> new RuntimeException("사용자 없음"));

                                Account a = Account.builder()
                                        .accountNo(acctNo)
                                        .bankCode(bankCode)
                                        .balance(0)
                                        .accountType(RegistAccountType.EXTERNAL) // 외부계좌
                                        .alias(null)                            // 별칭 없음
                                        .primary(false)                         // 주계좌 아님
                                        .balance(0)
                                        .createdAt(LocalDateTime.now())
                                        .registered(false)                      // 아직 등록되지 않음
                                        .member(member)
                                        .build();

                                // 블로킹 JPA 호출
                                return accountRepository.save(a);
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            // 3) 저장 완료 후에도 SSAFY 원본 Map 그대로 흘려줌
                            .thenReturn(resp);
                });
    }

    //계좌 목록 조회
    public Mono<Map<String,Object>> inquireAccountList(String userKey) {
        return client.inquireAccountList(userKey);
    }

    //계좌 입금(충전하기)
    public Mono<Map<String,Object>> depositAccount(String userKey, String accountNo, long transactionBalance, String transactionSummary) {
        return client.depositAccount(userKey, accountNo, transactionBalance, transactionSummary)
                .flatMap(resp ->
                        // 1) SSAFY 입금 API 호출이 성공했으면
                        Mono.fromCallable(() -> {
                                    // 2) 로컬 DB에서 accountNo에 해당하는 계좌 가져오기
                                    Account acct = accountRepository.findByAccountNo(accountNo)
                                            .orElseThrow(() -> new RuntimeException("계좌 없음: " + accountNo));
                                    // 3) balance 업데이트
                                    int updatedBalance = acct.getBalance() + (int) transactionBalance;
                                    acct.setBalance(updatedBalance);
                                    // 4) 저장
                                    accountRepository.save(acct);

                                    // 계좌 balance 갱신 완료

                                    // 6) 원본 SSAFY 응답 Map 그대로 리턴
                                    return resp;

                                })
                                .subscribeOn(Schedulers.boundedElastic())   // JPA 블로킹 호출은 별도 스케줄러에서
                                // 5) 저장이 끝나면 원본 SSAFY resp Map을 그대로 흘려보냄
                                .thenReturn(resp)
                );
    }

    //계좌 이체
    public Mono<Map<String,Object>> transferAccount(String userKey, String depositAccountNo, long transactionBalance, String withdrawalAccountNo, String depositTransactionSummary, String withdrawalTransactionSummary) {
        return client.transferAccount( userKey, depositAccountNo, transactionBalance, withdrawalAccountNo, depositTransactionSummary, withdrawalTransactionSummary)
                .flatMap(resp ->
                        Mono.fromCallable(() -> {
                                    // 1) 출금 계좌 조회
                                    Account withdrawAcct = accountRepository.findByAccountNo(withdrawalAccountNo)
                                            .orElseThrow(() -> new RuntimeException("출금 계좌 없음: " + withdrawalAccountNo));
                                    int newWithdrawBalance = withdrawAcct.getBalance() - (int)transactionBalance;
                                    withdrawAcct.setBalance(newWithdrawBalance);
                                    accountRepository.save(withdrawAcct);
                                    // 2) 입금 계좌 조회
                                    Account depositAcct = accountRepository.findByAccountNo(depositAccountNo)
                                            .orElseThrow(() -> new RuntimeException("입금 계좌 없음: " + depositAccountNo));
                                    int newDepositBalance = depositAcct.getBalance() + (int)transactionBalance;
                                    depositAcct.setBalance(newDepositBalance);
                                    accountRepository.save(depositAcct);

                                    // 계좌 balance 동기화 완료

                                    // 5) 원본 SSAFY 응답 Map 반환
                                    return resp;

                                })
                                .subscribeOn(Schedulers.boundedElastic())
                );
    }

    //계좌 출금(보증금 관련)
    public Mono<Map<String,Object>> withdrawAccount(String userKey, String accountNo, long transactionBalance, String transactionSummary) {
        return client.withdrawAccount(userKey, accountNo, transactionBalance, transactionSummary)
                .flatMap(resp ->
                        Mono.fromCallable(() -> {
                                    // 1) 로컬 DB에서 계좌 조회
                                    Account acct = accountRepository.findByAccountNo(accountNo)
                                            .orElseThrow(() -> new RuntimeException("계좌 없음: " + accountNo));
                                    // 2) 출금 금액만큼 balance 차감
                                    int updatedBalance = acct.getBalance() - (int) transactionBalance;
                                    acct.setBalance(updatedBalance);
                                    // 3) 변경된 엔티티 저장
                                    accountRepository.save(acct);

                                    // 계좌 balance 갱신 완료

                                    //5) 원본 SSAFY 응답 Map 그대로 리턴
                                    return resp;
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                // 4) 저장이 완료되면 원본 SSAFY 응답(Map) 반환
                                .thenReturn(resp)
                );
    }


    // 결제 처리 (rentalId 추가)
    public Mono<Map<String,Object>> paymentAccount(
            String userKey,
            String depositAccountNo,
            long transactionBalance,
            String withdrawalAccountNo,
            String depositTransactionSummary,
            String withdrawalTransactionSummary,
            Long rentalId
    ) {
        return client.transferAccount(
                        userKey,
                        depositAccountNo,
                        transactionBalance,
                        withdrawalAccountNo,
                        depositTransactionSummary,
                        withdrawalTransactionSummary
                )
                .flatMap(ssafyResp ->
                        // 블로킹 작업은 boundedElastic 스케줄러에서 처리
                        Mono.fromCallable(() ->
                                        transactionTemplate.execute(status -> {
                                            try {
                                                // 1) 출금 계좌 조회 & balance 차감
                                                Account withdrawAcct = accountRepository.findByAccountNo(withdrawalAccountNo)
                                                        .orElseThrow(() -> new RuntimeException("출금 계좌 없음: " + withdrawalAccountNo));
                                                withdrawAcct.setBalance(withdrawAcct.getBalance() - (int)transactionBalance);
                                                accountRepository.save(withdrawAcct);

                                                // 2) 입금 계좌 조회 & balance 증가
                                                Account depositAcct = accountRepository.findByAccountNo(depositAccountNo)
                                                        .orElseThrow(() -> new RuntimeException("입금 계좌 없음: " + depositAccountNo));
                                                depositAcct.setBalance(depositAcct.getBalance() + (int)transactionBalance);
                                                accountRepository.save(depositAcct);

                                                // 3) 결제 완료 이벤트 발행
                                                RemittanceCompletedEvent event = new RemittanceCompletedEvent(rentalId);
                                                eventPublisher.publishEvent(event);
                                                log.info("결제 완료 – rentalId: {}, amount: {}", rentalId, transactionBalance);

                                                // 4) 최종적으로 SSAFY 응답 반환
                                                return ssafyResp;
                                            } catch (Exception e) {
                                                status.setRollbackOnly();
                                                throw e;
                                            }
                                        })
                                )
                                .subscribeOn(Schedulers.boundedElastic())
                );
    }

    // 계좌 거래 내역 조회
    public Mono<Map<String,Object>> inquireTransactionHistory(
            InquireTransactionHistoryRequestDto req
    ) {
        return client.inquireTransactionHistoryList(req);
    }

    // regist_account 서비스들을 병합
    // 등록된(registered=true) 모든 계좌 조회
    public Mono<List<RegistAccountResponseDto>> getRegistAccounts(String userKey) {
        return Mono.fromCallable(() ->
                accountRepository
                        .findByMember_UserKeyAndRegisteredIsTrue(userKey)
                        .stream()
                        .map(this::toDto)
                        .collect(Collectors.toList())
        ).subscribeOn(Schedulers.boundedElastic());
    }

    // 계좌 등록 - 외부(EXTERNAL)
    public Mono<RegistAccountResponseDto> registerAccount(RegistAccountRequestDto req) {
        return Mono.fromCallable(() -> {
            // 1) Member 조회
            Member member = memberQueryPort.findByUserKey(req.getUserKey())
                    .orElseThrow(() -> new RuntimeException("사용자 없음: " + req.getUserKey()));

            // 2) SSAFY 에 이미 만들어둔 account 엔티티 조회
            Account acct = accountRepository
                    .findByAccountNoAndBankCode(req.getAccountNo(), req.getBankCode())
                    .orElseThrow(() -> new RuntimeException(
                            "계좌 없음 또는 은행코드 불일치: "
                                    + req.getAccountNo() + " / " + req.getBankCode()));

            // 3) 이미 등록된 계좌인지 체크
            if (acct.getRegistered()) {
                throw new RuntimeException("이미 등록된 계좌입니다.");
            }

            // 4) 주계좌 판별: 같은 회원의 EXTERNAL 계좌 중 이미 primary=true 가 있는지
            boolean hasPrimary = accountRepository
                    .findByMember_UserKeyAndAccountType(req.getUserKey(), RegistAccountType.EXTERNAL)
                    .stream().anyMatch(Account::getPrimary);
            boolean isPrimary = !hasPrimary;

            // 5) 엔티티 업데이트 후 저장
            acct.setRegistered(true);
            acct.setAccountType(RegistAccountType.EXTERNAL);
            acct.setAlias(req.getAlias());
            acct.setPrimary(isPrimary);
            acct.setCreatedAt(LocalDateTime.now());  // 등록 시각으로 덮어쓰기
            Account saved = accountRepository.save(acct);

            return toDto(saved);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 빌리페이 자동 생성 & 등록 (type=BILI_PAY)
     * 사용자 계정 생성 직후 자동 호출: 계좌 생성 & DB 저장 → Account 생성
     * 빌리페이 생성 메서드, 따로 api 는 필요없다. 계정 생성하고 service로 요청해서 만들면 끝
     */
    public Mono<RegistAccountResponseDto> createBilipay(String userKey) {
        return client.createAccount(userKey, /* 빌리페이 고정 파라미터 */ "999-1-6c20074711854e")
                .flatMap(resp -> {
                    @SuppressWarnings("unchecked")
                    Map<String,Object> rec = (Map<String,Object>) resp.get("REC");
                    if (rec == null) {
                        return Mono.error(new RuntimeException("SSAFY 계좌 생성 응답에 REC가 없습니다."));
                    }
                    String acctNo   = rec.get("accountNo").toString();
                    String bankCode = rec.get("bankCode").toString();

                    return Mono.fromCallable(() -> {
                        Member member = memberQueryPort.findByUserKey(userKey)
                                .orElseThrow(() -> new RuntimeException("사용자 없음: " + userKey));

                        // 1) Account 엔티티 생성
                        Account a = Account.builder()
                                .accountNo(acctNo)
                                .bankCode(bankCode)
                                .balance(0)
                                .createdAt(LocalDateTime.now())
                                .member(member)
                                .registered(true)        // 자동 등록
                                .accountType(RegistAccountType.BILI_PAY)
                                .alias("빌리페이")
                                .primary(false)           // 빌리페이는 주계좌 아님
                                .build();
                        a = accountRepository.save(a);
                        return toDto(a);
                    }).subscribeOn(Schedulers.boundedElastic());
                });
    }

    /** 4) 주계좌 변경: EXTERNAL 계좌만 대상 */
    public Mono<RegistAccountResponseDto> changePrimary(String userKey, Long targetAccountId) {
        return Mono.fromCallable(() -> {
            Account target = accountRepository.findById(targetAccountId)
                    .orElseThrow(() -> new RuntimeException("계좌 없음: " + targetAccountId));
            if (!target.getMember().getUserKey().equals(userKey)) {
                throw new RuntimeException("권한 없음: 다른 사용자 계좌 변경 불가");
            }
            if (target.getAccountType() != RegistAccountType.EXTERNAL) {
                throw new RuntimeException("BILI_PAY 계좌는 주계좌로 설정할 수 없습니다.");
            }

            // 1) 기존 primary=false 처리
            accountRepository
                    .findByMember_UserKeyAndAccountType(userKey, RegistAccountType.EXTERNAL)
                    .forEach(a -> {
                        if (a.getPrimary()) {
                            a.setPrimary(false);
                            accountRepository.save(a);
                        }
                    });

            // 2) 대상만 primary=true
            target.setPrimary(true);
            target.setCreatedAt(LocalDateTime.now()); // Optional: 갱신 시각
            Account saved = accountRepository.save(target);
            return toDto(saved);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** DTO 변환 헬퍼 */
    private RegistAccountResponseDto toDto(Account a) {
        return new RegistAccountResponseDto(
                a.getId(),
                a.getAccountNo(),
                a.getBankCode(),
                a.getAccountType(),
                a.getAlias(),
                a.getPrimary(),
                a.getBalance(),
                a.getCreatedAt()
        );
    }
}
