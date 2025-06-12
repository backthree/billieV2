package com.nextdoor.nextdoor.domain.fintech.port;

import com.nextdoor.nextdoor.domain.member.domain.model.Member;

import java.util.Optional;

/**
 * Port for querying Member entities from the fintech domain
 */
public interface MemberQueryPort {
    /**
     * Find a member by userKey
     * @param userKey the userKey of the member
     * @return the member, or empty if not found
     */
    Optional<Member> findByUserKey(String userKey);

    /**
     * Find a member by ID
     * @param id the ID of the member
     * @return the member, or empty if not found
     */
    Optional<Member> findById(Long id);
}