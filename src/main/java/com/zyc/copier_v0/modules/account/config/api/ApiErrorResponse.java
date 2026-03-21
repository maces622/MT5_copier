package com.zyc.copier_v0.modules.account.config.api;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApiErrorResponse {

    private final String code;
    private final String message;
    private final Instant timestamp = Instant.now();
}
