package com.payflow.auth.internal.util;

import com.payflow.auth.internal.domain.User;
import com.payflow.auth.internal.dtos.UserDTO;

public class HelperUtility {

    public static UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getWalletTag()
        );
    }
}
