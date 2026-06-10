package com.payflow.auth.internal.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class TokenHashingService {
    private final String hashed;

    public TokenHashingService(@Value("${refresh-hashed}") String hashed) {
        this.hashed = hashed;
    }

    public String getHashedToken(String token){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA256");
            byte[] encodedToken = md.digest((hashed+token).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedToken);
        }catch (Exception e){
            throw new RuntimeException("Failed hashing token");
        }
    }
}
