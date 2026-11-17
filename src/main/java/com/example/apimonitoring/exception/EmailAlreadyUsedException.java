package com.example.apimonitoring.exception;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Email is already in use: " + email);
    }
}
