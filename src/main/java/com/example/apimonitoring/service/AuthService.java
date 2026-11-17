package com.example.apimonitoring.service;

import com.example.apimonitoring.dto.AuthResponse;
import com.example.apimonitoring.dto.LoginRequest;
import com.example.apimonitoring.dto.RegisterRequest;
import com.example.apimonitoring.entity.User;
import com.example.apimonitoring.exception.EmailAlreadyUsedException;
import com.example.apimonitoring.exception.UserNotFoundException;
import com.example.apimonitoring.repository.UserRepository;
import com.example.apimonitoring.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public User register(RegisterRequest request) {

        String email = request.email(); // already normalized by RegisterRequest

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        return userRepository.save(user);
    }

    /**
     * Checks email + password and returns a JWT.
     * A wrong email and a wrong password give the same BadCredentialsException,
     * so the API does not reveal which emails are registered.
     */
    public AuthResponse login(LoginRequest request) {

        String email = request.email(); // already normalized by LoginRequest

        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return new AuthResponse(
                tokenService.generateToken(user),
                "Bearer",
                tokenService.expirationSeconds()
        );
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
