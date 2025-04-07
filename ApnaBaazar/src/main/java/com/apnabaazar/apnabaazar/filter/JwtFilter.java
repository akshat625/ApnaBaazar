package com.apnabaazar.apnabaazar.filter;

import com.apnabaazar.apnabaazar.exceptions.InvalidTokenException;
import com.apnabaazar.apnabaazar.service.JwtService;
import com.apnabaazar.apnabaazar.service.TokenBlacklistService;
import com.apnabaazar.apnabaazar.service.UserDetailsServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class JwtFilter  extends OncePerRequestFilter{
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtService  jwtService;


    @Autowired
    private TokenBlacklistService  tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        // Skip filter for refresh token endpoint
        if (request.getRequestURI().startsWith("/auth/refresh-token") ||
                request.getRequestURI().startsWith("/auth/logout")) {
            chain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");
        String username = null;
        String token = null;

        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);

                // Check for blacklist token
                if (tokenBlacklistService.isAccessTokenBlacklisted(token)) {
                    sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token has been invalidated");
                    return;
                }

                // Extract username - will throw exception if token is malformed
                username = jwtService.extractUsername(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtService.validateToken(token, "access", username)) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "JWT token has expired");
        } catch (InvalidTokenException | MalformedJwtException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing the token");
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
