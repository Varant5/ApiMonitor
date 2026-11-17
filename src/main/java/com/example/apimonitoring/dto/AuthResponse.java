package com.example.apimonitoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access token returned after a successful login")
public record AuthResponse(

        @Schema(description = "JWT to send as 'Authorization: Bearer <token>'",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOi...")
        String accessToken,

        @Schema(description = "Always 'Bearer'", example = "Bearer")
        String tokenType,

        @Schema(description = "Token lifetime in seconds", example = "3600")
        long expiresIn
) {
}
