package com.payflow.auth.internal.util;

import com.payflow.auth.internal.domain.User;
import com.payflow.auth.internal.dtos.UserDTO;

import java.security.SecureRandom;
import java.util.Base64;

public class HelperUtility {
    private final static SecureRandom secureRandom = new SecureRandom();

    public static UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getWalletTag(),
                user.getMpesaPhoneNumber()
        );
    }

    public static String generateRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
