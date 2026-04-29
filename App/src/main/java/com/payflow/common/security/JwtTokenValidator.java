package com.payflow.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtTokenValidator {
    private final String secretKey;

    public JwtTokenValidator(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    private SecretKey getKey() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(bytes);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .requireIssuer("auth")
                .requireAudience("wallet")
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isTokenValid(String token) {
        return (!isTokenExpired(token));
    }

    public Long extractUserId(String authToken) {
        Claims claims = extractAllClaims(authToken);

        return claims.get("userId", Long.class);
    }

    public String extractUserRole(String authToken) {
        Claims claims = extractAllClaims(authToken);

        return claims.get("role", String.class);
    }
}
