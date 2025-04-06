package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.repository.AuthTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Getter
@Setter
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration.activation}")
    private long activationTokenExpirationTime;

    @Value("${jwt.expiration.access}")
    private long accessTokenExpirationTime;

    @Value("${jwt.expiration.refresh}")
    private long refreshTokenExpirationTime;

    @Value("${jwt.expiration.forgot}")
    private long forgotPasswordTokenExpirationTime;

    @Autowired
    private AuthTokenRepository authTokenRepository;


    /**
     * Retrieves the signing key from the secret.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extracts all claims from the token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the username (subject) from the token.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts expiration date from token.
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Checks if the token is expired.
     */

    Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates if the token is valid.
     */
    public Boolean validateToken(String token, String expectedType, String username) {
        try {
            Claims claims = extractAllClaims(token);
            String email = extractUsername(token);
            return (email.equals(username) && !isTokenExpired(token) && expectedType.equals(claims.get("type")));
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Expired JWT Token");
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT Token");
        }
    }

    /**
     * Invalidates the existing token
     */
//    public void invalidateToken(String token) {
//        Claims claims = extractAllClaims(token);
//        claims.setExpiration(new Date(System.currentTimeMillis() - 1000));
//
//    }
    public String generateActivationToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "activation")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + activationTokenExpirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateAccessToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateforgotPasswordToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "forgot")
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusMillis(forgotPasswordTokenExpirationTime)))
                .signWith(getSigningKey())
                .compact();
    }

    public String getTokenType(String token) {
        return extractAllClaims(token).get("type").toString();
    }

}

