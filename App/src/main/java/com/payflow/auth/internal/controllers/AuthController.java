package com.payflow.auth.internal.controllers;

import com.payflow.auth.internal.dtos.UserDTO;
import com.payflow.auth.internal.services.AuthService;
import com.payflow.auth.internal.util.*;
import com.payflow.auth.internal.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        return ResponseEntity.ok(authService.register(registrationRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AccessToken> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest.email(),loginRequest.password()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMe(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        Long userId = user.getUserId();

        return ResponseEntity.ok(authService.getMe(userId));
    }

    @PutMapping("/update/details")
    public ResponseEntity<UpdateDetailsResponse> updateDetails(Authentication authentication,@Valid @RequestBody UpdateDetailsRequest request) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        Long userId = user.getUserId();

        return ResponseEntity.ok(authService.updateDetails(userId,request));
    }

    @PutMapping("/update/password")
    public ResponseEntity<UpdatePasswordResponse> updatePassword(Authentication authentication,@Valid @RequestBody UpdatePasswordRequest request) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        Long userId = user.getUserId();

        return ResponseEntity.ok(authService.updatePassword(userId,request));
    }
}
