package com.example.apimonitoring.dto;

import com.example.apimonitoring.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "User account (never contains the password)")
public record UserResponse(

        @Schema(example = "1")
        Long id,

        @Schema(example = "jan.kowalski@example.com")
        String email,

        @Schema(example = "2026-11-17T10:15:30Z")
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}
