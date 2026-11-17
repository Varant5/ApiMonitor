package com.example.apimonitoring.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockJwtUserSecurityContextFactory implements WithSecurityContextFactory<WithMockJwtUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockJwtUser annotation) {

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(String.valueOf(annotation.userId()))
                .claim("email", annotation.email())
                .build();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        return context;
    }
}
