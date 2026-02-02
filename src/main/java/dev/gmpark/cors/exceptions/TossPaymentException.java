package dev.gmpark.cors.exceptions;

import lombok.Getter;

@Getter
public class TossPaymentException extends RuntimeException {
    private final String code;
    private final String message;

    public TossPaymentException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
