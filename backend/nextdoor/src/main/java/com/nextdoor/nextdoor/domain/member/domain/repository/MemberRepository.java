package com.nextdoor.nextdoor.domain.member.domain.repository;

import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByUserKey(String userKey);
}
