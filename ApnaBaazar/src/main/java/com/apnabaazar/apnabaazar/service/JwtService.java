package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.ExpiredTokenException;
import com.apnabaazar.apnabaazar.exceptions.InvalidTokenException;
import com.apnabaazar.apnabaazar.repository.AuthTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
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

    @Value("${jwt.expiration.reset}")
    private long resetPasswordTokenExpirationTime;

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
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid token");
        }
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

    public String extractIssuer(String token) {
        return extractAllClaims(token).getIssuer();
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


    public boolean validateToken(String token, String tokenType,String username) {
        final String email = extractUsername(token);
        final Claims claims = extractAllClaims(token);

        if (!username.equals(email)) {
            throw new InvalidTokenException("Token does not match the provided user.");
        }

        String typeFromToken = (String) claims.get("type");
        if (!tokenType.equals(typeFromToken)) {
            throw new InvalidTokenException("Token type mismatch. Expected: " + tokenType + ", but found: " + typeFromToken);
        }

        if (isTokenExpired(token)) {
            throw new ExpiredTokenException("Token has expired.");
        }


        return true;
    }


    public String generateActivationToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "activation")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + activationTokenExpirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateAccessToken(String email, String sessionId) {
        return Jwts.builder()
                .setIssuer(sessionId)
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

    public String generateResetPasswordToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "forgot")
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusMillis(resetPasswordTokenExpirationTime)))
                .signWith(getSigningKey())
                .compact();
    }

    public String getTokenType(String token) {
        return extractAllClaims(token).get("type").toString();
    }

}

