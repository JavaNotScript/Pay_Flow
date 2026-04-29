package com.payflow.auth.internal.util;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8,max = 22) String password,
        @NotBlank @Size(min = 3,max = 20) String firstName,
        @NotBlank @Size(min = 3,max = 20) String lastName
) {
}
