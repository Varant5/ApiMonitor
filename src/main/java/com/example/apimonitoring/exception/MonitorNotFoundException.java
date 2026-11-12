package com.example.apimonitoring.exception;

public class MonitorNotFoundException extends RuntimeException {

    public MonitorNotFoundException(String message) {
        super(message);
    }
}