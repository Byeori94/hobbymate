package com.byeori.hobbymate.common.exception;

public class ProfileImageProcessingException extends RuntimeException {

    public ProfileImageProcessingException(String message) {
        super(message);
    }

    public ProfileImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
