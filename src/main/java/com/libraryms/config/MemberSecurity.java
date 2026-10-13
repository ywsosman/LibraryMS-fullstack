package com.libraryms.config;

import com.libraryms.model.Member;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


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
