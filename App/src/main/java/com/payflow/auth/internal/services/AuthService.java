package com.payflow.auth.internal.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.auth.internal.domain.RoleType;
import com.payflow.auth.internal.domain.User;
import com.payflow.auth.internal.domain.UserRole;
import com.payflow.auth.internal.dtos.UserDTO;
import com.payflow.auth.internal.repos.RoleRepository;
import com.payflow.auth.internal.repos.UserRepository;
import com.payflow.auth.internal.security.JwtTokenGenerator;
import com.payflow.auth.internal.util.*;
import com.payflow.common.domain.EventType;
import com.payflow.common.ex.RoleNotFoundEx;
import com.payflow.common.ex.UserRegistrationEx;
import com.payflow.auth.internal.security.AuthenticatedUser;
import com.payflow.outbox.OutboxEvent;
import com.payflow.outbox.OutboxRepository;
import com.payflow.outbox.StatusEnum;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenGenerator jwtService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PasswordEncoder passwordEncoder;

    public UserDTO register(RegistrationRequest registrationRequest) {
        logger.info("Attempting User registration using email={}", registrationRequest.email());

        if (userRepository.existsByEmail(registrationRequest.email())) {
            logger.info("User already exists with email={}", registrationRequest.email());
            throw new UsernameNotFoundException("Email already in use");
        }


        try {
            User user = new User();

            user.setEmail(registrationRequest.email());
            user.setPassword(passwordEncoder.encode(registrationRequest.password()));
            user.setFirstName(registrationRequest.firstName());
            user.setLastName(registrationRequest.lastName());
            user.setIsLocked(false);
            user.setEnabled(true);
            user.setIsCredentialsExpired(false);

            UserRole role = roleRepository.findByRoleName(RoleType.USER).orElseThrow(() -> new RoleNotFoundEx("Role not found"));

            user.setRole(role);

            User createdUser = userRepository.save(user);


            //Record user created to assist dealing with orphan wallets in case of wallet creation fails
            JsonNode payload = objectMapper.valueToTree(
                    Map.of("userId", createdUser.getUserId()));

            OutboxEvent event = new OutboxEvent();
            event.setUserId(createdUser.getUserId());
            event.setPayload(payload);
            event.setEventType(EventType.USER_CREATED);
            event.setStatus(StatusEnum.PENDING);

            outboxRepository.save(event);

            logger.info("User created with email={}", createdUser.getEmail());
            return HelperUtility.convertToDTO(createdUser);

        } catch (Exception e) {
            throw new UserRegistrationEx("User registration failed.");
        }
    }

    public AccessToken login(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        if (authentication.isAuthenticated()) {
            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();

            String token = jwtService.generateToken(user.getUserId(), user.getUsername(), user.getAuthorities());

            return new AccessToken(token);
        }

        throw new BadCredentialsException("Incorrect email/password.");
    }

    public UserDTO getMe(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return HelperUtility.convertToDTO(user);
    }

    @Transactional
    public UpdateDetailsResponse updateDetails(Long userId, @Valid UpdateDetailsRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());


        return new UpdateDetailsResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }

    @Transactional
    public UpdatePasswordResponse updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!request.newPassword().equals(request.confirmPassword())) {
            logger.error("Passwords do not match");
            throw new BadCredentialsException("Passwords do not match");
        } else if (passwordEncoder.matches(request.confirmPassword(), user.getPassword())) {
            logger.error("oldPassword/newPassword match");
            throw new BadCredentialsException("New password can't be same as old password");
        }

        user.setPassword(passwordEncoder.encode(request.confirmPassword()));
        return new UpdatePasswordResponse("Password updated successfully.");
    }
}
