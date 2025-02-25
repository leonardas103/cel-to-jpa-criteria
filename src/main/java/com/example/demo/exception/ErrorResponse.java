package com.example.demo.exception;

public class ErrorResponse {
    private int status;
    private String message;

    // Default constructor (required for Jackson)
    public ErrorResponse() {}

    // Parameterized constructor
    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    // Getters and Setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}