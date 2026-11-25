package com.flexmeet.service.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate JWT token
     */
    public String generateToken(String userId, String email, String username) {
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generate token with custom claims
     */
    public String generateTokenWithClaims(String userId, String email, String username, 
                                          java.util.Map<String, Object> claims) {
        JwtBuilder builder = Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512);

        if (claims != null) {
            claims.forEach(builder::claim);
        }

        return builder.compact();
    }

    /**
     * Get user ID from token
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (JwtException ex) {
            log.error("Failed to get user ID from token", ex);
            return null;
        }
    }

    /**
     * Get email from token
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("email", String.class);
        } catch (JwtException ex) {
            log.error("Failed to get email from token", ex);
            return null;
        }
    }

    /**
     * Get all claims from token
     */
    public Claims getAllClaimsFromToken(String token) {
        return parseToken(token);
    }

    /**
     * Validate token
     */
    public Boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Parse token
     */
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getPayload();
    }

    /**
     * Get authorities from token
     */
    public List<GrantedAuthority> getAuthoritiesFromToken(String token) {
        return new java.util.ArrayList<>(Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_USER")
        ));
    }
}
