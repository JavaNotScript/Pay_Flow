package com.payflow.auth.api;

import com.payflow.auth.internal.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthAdapter implements AuthFacade {


    @Override
    public Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthorized access");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Unauthorized access");
        }
        return user.getUserId();
    }

}
