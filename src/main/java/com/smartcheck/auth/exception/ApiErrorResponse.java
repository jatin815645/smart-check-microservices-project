package com.smartcheck.auth.exception;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
}

