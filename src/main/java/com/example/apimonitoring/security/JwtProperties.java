package com.example.apimonitoring.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT settings from application.properties (prefix "security.jwt").
 *
 * @param secret     Base64-encoded HMAC key, at least 256 bits (32 bytes) for HS256
 * @param expiration how long an access token is valid
 * @param issuer     value of the "iss" claim; tokens with a different issuer are rejected
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {
}
