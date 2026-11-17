package com.example.apimonitoring.controller;

import com.example.apimonitoring.dto.AuthResponse;
import com.example.apimonitoring.dto.LoginRequest;
import com.example.apimonitoring.dto.RegisterRequest;
import com.example.apimonitoring.dto.UserResponse;
import com.example.apimonitoring.exception.ErrorResponse;
import com.example.apimonitoring.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Create an account and obtain an access token")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid email or too short password",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email is already registered",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 409,
                                      "message": "Email already in use",
                                      "errors": ["Email is already in use: jan.kowalski@example.com"]
                                    }
                                    """)))
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(authService.register(request));
    }

    @Operation(
            summary = "Log in",
            description = "Returns a JWT. Send it in the `Authorization: Bearer <token>` header "
                    + "(in Swagger UI: the **Authorize** button)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged in",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Wrong email or password",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 401,
                                      "message": "Unauthorized",
                                      "errors": ["Invalid email or password"]
                                    }
                                    """)))
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
