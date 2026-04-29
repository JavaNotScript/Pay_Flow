package com.payflow.auth.internal.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;

@Service
public class JwtTokenGenerator {
    private final String jwtSecret;
    private final Long jwtExpiration;

    public JwtTokenGenerator(@Value("${jwt.secret}") String jwtSecret, @Value("${jwt.expiration}") Long jwtExpiration) {
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
    }

    public SecretKey getKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String username, Collection<? extends GrantedAuthority> authorities) {
        String auth = authorities
                .stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        return Jwts.builder()
                .subject(username)
                .claims()
                .add("userId", userId)
                .add("role", auth)
                .setIssuer("auth")
                .setAudience("wallet")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .and()
                .signWith(getKey())
                .compact();
    }


}
