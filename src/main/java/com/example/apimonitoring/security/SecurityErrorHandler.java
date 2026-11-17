package com.example.apimonitoring.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Security errors happen in filters, before any controller runs, so GlobalExceptionHandler never sees them.
 * This class writes them in the same {status, message, errors} format as all other API errors.
 */
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    /** 401 — no token, expired token, invalid signature, ... */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized", "Missing or invalid access token");
    }

    /** 403 — authenticated, but not allowed to do this. */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        write(response, HttpServletResponse.SC_FORBIDDEN,
                "Forbidden", "You do not have permission to access this resource");
    }

    // Messages are fixed constants (no user input), so building the JSON by hand is safe here.
    private void write(HttpServletResponse response, int status, String message, String error) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"status\":%d,\"message\":\"%s\",\"errors\":[\"%s\"]}".formatted(status, message, error)
        );
    }
}
