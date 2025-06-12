package com.nextdoor.nextdoor.domain.member.infrastructure.persistence.adapter;

import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import com.nextdoor.nextdoor.domain.member.infrastructure.persistence.jpa.MemberJpaRepository;
import com.nextdoor.nextdoor.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Member save(Member member) {
        return memberJpaRepository.save(member);
    }

    @Override
    public Optional<Member> findById(Long id) {
        return memberJpaRepository.findById(id);
    }

    @Override
    public Optional<Member> findByUserKey(String userKey) {
        return memberJpaRepository.findByUserKey(userKey);
    }
}
