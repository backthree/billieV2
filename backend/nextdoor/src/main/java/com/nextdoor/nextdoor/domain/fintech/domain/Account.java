package com.nextdoor.nextdoor.domain.fintech.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    @Column(name = "account_no", nullable = false, length = 30)
    private String accountNo;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private RegistAccountType accountType;

    @Column(length = 50)
    private String alias;

    @Column(name = "is_primary", nullable = false)
    private Boolean primary;

    @Column(nullable = false)
    private Integer balance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 신규 추가: 사용자가 “등록 완료” 처리를 했는지 여부 **/
    @Column(name = "is_registered", nullable = false)
    private Boolean registered;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnore
    private Member member;
}
