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
import java.util.Date;

@Getter
@Setter
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    /**
     * Generates a JWT token with the given username.
     */
//    public String generateToken(String username, String tokenType) {
//        Map<String, Object> claims = new HashMap<>();
//        return createToken(claims, username, tokenType);
//    }
//    /**
//     * Creates a signed JWT token.
//     */
//    private String createToken(Map<String, Object> claims, String subject, String tokenType) {
//        return Jwts.builder()
//
//                .claim("type", tokenType)
//                .setSubject(subject)
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
//                .signWith(getSigningKey())
//                .compact();
//    }

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
    public Boolean validateToken(String token,String username) {
        try {
            String email = extractUsername(token);
            return (email.equals(username) && !isTokenExpired(token));
        }catch(ExpiredJwtException e){
            throw new RuntimeException("Expired JWT Token");
        }catch(Exception e){
            throw new RuntimeException("Invalid JWT Token");
        }
    }

    /**
     *Invalidates the existing token
     */
//    public void invalidateToken(String token) {
//        Claims claims = extractAllClaims(token);
//        claims.setExpiration(new Date(System.currentTimeMillis() - 1000));
//
//    }

    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "activation")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

}

