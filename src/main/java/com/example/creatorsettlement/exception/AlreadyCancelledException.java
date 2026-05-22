package com.example.creatorsettlement.exception;

public class AlreadyCancelledException extends RuntimeException {
    public AlreadyCancelledException(String message) {
        super(message);
    }
}
