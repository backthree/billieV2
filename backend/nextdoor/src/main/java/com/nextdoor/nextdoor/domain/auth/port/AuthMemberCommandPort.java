package com.nextdoor.nextdoor.domain.auth.port;

import com.nextdoor.nextdoor.domain.auth.dto.MemberCommandDto;
import com.nextdoor.nextdoor.domain.auth.dto.MemberQueryDto;

public interface AuthMemberCommandPort {

    MemberQueryDto save(MemberCommandDto command);
}
