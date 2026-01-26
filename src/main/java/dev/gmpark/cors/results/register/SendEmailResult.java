package dev.gmpark.cors.results.register;

public enum SendEmailResult {
    FAILURE,
    FAILURE_EMAIL_DUPLICATE,
    FAILURE_EMAIL_NOT_FOUND, // 추가
    SUCCESS
}
