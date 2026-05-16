package com.payflow.auth.internal.util;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank @Size(min = 8) String newPassword,
        @NotBlank @Size(min = 8) String confirmPassword
) {
}
