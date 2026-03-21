package com.zyc.copier_v0.modules.account.config.api;

import java.time.Instant;

public class ApiErrorResponse {

    private final String code;
    private final String message;
    private final Instant timestamp = Instant.now();

    public ApiErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
