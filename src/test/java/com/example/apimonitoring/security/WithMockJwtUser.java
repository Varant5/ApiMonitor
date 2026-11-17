package com.example.apimonitoring.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Runs a test as a user logged in with a JWT, like {@code @WithMockUser} but with a real {@code Jwt} principal,
 * so {@link CurrentUserId} resolves the user id exactly as in production.
 *
 * <pre>{@code
 * @WithMockJwtUser(userId = 1)
 * class MonitorControllerTest { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockJwtUserSecurityContextFactory.class)
public @interface WithMockJwtUser {

    long userId() default 1L;

    String email() default "user@example.com";
}
