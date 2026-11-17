package com.example.apimonitoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login credentials")
public record LoginRequest(

        @Schema(example = "jan.kowalski@example.com")
        @NotBlank(message = "Email is required")
        String email,

        @Schema(example = "Secret123!")
        @NotBlank(message = "Password is required")
        String password
) {

    public LoginRequest {
        email = EmailNormalizer.normalize(email);
    }
}
