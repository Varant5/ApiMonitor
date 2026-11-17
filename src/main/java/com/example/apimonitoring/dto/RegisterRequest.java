package com.example.apimonitoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Data required to create an account")
public record RegisterRequest(

        @Schema(description = "Email used to log in", example = "jan.kowalski@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Password, 8-72 characters", example = "Secret123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {

    /**
     * Runs when Jackson creates the object — before @Valid checks it.
     * So " Jan@Example.COM " is validated (and stored) as "jan@example.com".
     */
    public RegisterRequest {
        email = EmailNormalizer.normalize(email);
    }
}
