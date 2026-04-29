package com.payflow.auth.api;

import org.springframework.security.core.Authentication;

public interface AuthFacade {

    Long extractUserId(Authentication authentication);
}
