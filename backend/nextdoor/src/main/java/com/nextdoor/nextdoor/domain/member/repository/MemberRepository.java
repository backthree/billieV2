package com.nextdoor.nextdoor.domain.member.repository;

import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUserKey(String userKey);
}
