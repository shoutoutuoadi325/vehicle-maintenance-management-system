package org.com.repair.exception;

public class GamificationException extends RuntimeException {

    private final GamificationErrorCode errorCode;

    public GamificationException(GamificationErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GamificationErrorCode getErrorCode() {
        return errorCode;
    }
}