package com.anderson.financialmanager.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        URI type,
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp,
        List<ErroValidacaoCampo> errors
) {
    public static ApiErrorResponse of(int status, String title, String detail, String path) {
        return new ApiErrorResponse(
                URI.create("https://financialmanager.anderson.com/errors/" + status),
                title,
                status,
                detail,
                path,
                Instant.now(),
                null
        );
    }

    public static ApiErrorResponse ofValidation(int status, String title, String detail, String path, List<ErroValidacaoCampo> errors) {
        return new ApiErrorResponse(
                URI.create("https://financialmanager.anderson.com/errors/validation"),
                title,
                status,
                detail,
                path,
                Instant.now(),
                errors
        );
    }
}
