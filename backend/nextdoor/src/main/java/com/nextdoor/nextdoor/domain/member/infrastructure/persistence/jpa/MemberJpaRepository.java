package com.nextdoor.nextdoor.domain.member.infrastructure.persistence.jpa;

import com.nextdoor.nextdoor.domain.member.domain.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUserKey(String userKey);
}
