package com.libraryms.member.security;

import com.libraryms.member.entity.Member;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.libraryms.common.security.CurrentUserPrincipal;

@Component("memberSecurity")
public class MemberSecurity {

    public boolean isCurrentMember(Authentication authentication, Long memberId) {
        if (authentication == null || memberId == null) {
            return false;
        }
        if (authentication.getPrincipal() instanceof CurrentUserPrincipal principal) {
            return memberId.equals(principal.memberId());
        }
        return false;
    }
}
