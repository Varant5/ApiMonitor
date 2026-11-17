package com.example.apimonitoring.controller;

import com.example.apimonitoring.dto.UserResponse;
import com.example.apimonitoring.security.CurrentUserId;
import com.example.apimonitoring.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final AuthService authService;

    @Operation(summary = "Get the currently logged-in user")
    @GetMapping("/me")
    public UserResponse me(@CurrentUserId Long userId) {
        return UserResponse.from(authService.getUserById(userId));
    }
}
