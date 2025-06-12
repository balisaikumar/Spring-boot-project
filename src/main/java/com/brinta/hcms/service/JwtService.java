package com.brinta.hcms.service;


import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.exception.JwtExpiredException;
import com.brinta.hcms.exception.JwtInvalidException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpirationMs;


    public TokenPair generateTokenPair(Authentication authentication) {
        String accessToken = generateAccessToken(authentication);
        String refreshToken = generateRefreshToken(authentication);

        return new TokenPair(accessToken, refreshToken);
    }

    // Generate access token
    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, jwtExpirationMs, new HashMap<>());
    }

    // Generate refresh token
    public String generateRefreshToken(Authentication authentication) {
        Map<String, String> claims = new HashMap<>();
        claims.put("tokenType", "refresh");
        return generateToken(authentication, refreshExpirationMs, claims);
    }

    private String generateToken(Authentication authentication, long expirationInMs, Map<String, String> claims) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        Date now = new Date(); // Time of token creation
        Date expiryDate = new Date(now.getTime() + expirationInMs); // Time of token expiration

        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .and()
                .subject(userPrincipal.getUsername())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSignInKey())
                .compact();
    }

    // Validate token
    public boolean validateTokenForUser(String token, UserDetails userDetails) {
        final String username = extractUsernameFromToken(token);
        return username != null
                && username.equals(userDetails.getUsername());
    }

    public boolean isValidToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new JwtExpiredException("Token has expired", e); // Custom exception
        } catch (JwtException e) {
            throw new JwtInvalidException("Token is invalid", e); // Custom exception
        }
    }

    public String extractUsernameFromToken(String token) {
        Claims claims = extractAllClaims(token);

        if(claims != null) {
            return claims.getSubject();
        }
        return null;

    }

    // Validate if the token is refresh token
    public boolean isRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        if(claims == null) {
            return false;
        }
        return "refresh".equals(claims.get("tokenType"));
    }

    private Claims extractAllClaims(String token) {
        Claims claims = null;

        try {
            claims = Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }  catch (ExpiredJwtException ex) {
            // ✅ Expired token
            throw new JwtExpiredException("Token expired", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            // ✅ Malformed or tampered token
            throw new JwtInvalidException("Token invalid", ex);
        }
        return claims;
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}