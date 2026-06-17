package com.payflow.auth.api;

import com.payflow.auth.internal.domain.User;
import com.payflow.auth.internal.dtos.UserDTO;
import com.payflow.auth.internal.repos.UserRepository;
import com.payflow.auth.internal.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthAdapter implements AuthFacade {
    private final UserRepository userRepository;

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

    public UserDTO getUserInfoByUserId(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new UserDTO(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getWalletTag(),
                user.getMpesaPhoneNumber()
        );
    }
}
