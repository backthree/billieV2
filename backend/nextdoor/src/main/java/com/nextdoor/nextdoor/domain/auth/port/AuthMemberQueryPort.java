package com.nextdoor.nextdoor.domain.auth.port;

import com.nextdoor.nextdoor.domain.auth.dto.MemberQueryDto;

import java.util.Optional;

public interface AuthMemberQueryPort {

    Optional<MemberQueryDto> findById(Long id);

    Optional<MemberQueryDto> findByEmailAndAuthProvider(String email, String authProvider);
}
