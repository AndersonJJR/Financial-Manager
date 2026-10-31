package com.anderson.financialmanager.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleRecursoNaoEncontrado(
            RecursoNaoEncontradoException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Recurso não encontrado",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ApiErrorResponse> handleRegraDeNegocio(
            RegraDeNegocioException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Violação de regra de negócio",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleAcessoNegado(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "Acesso negado",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Credenciais inválidas",
                "E-mail ou senha incorretos.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ErroValidacaoCampo> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ErroValidacaoCampo(err.getField(), err.getDefaultMessage()))
                .toList();

        ApiErrorResponse error = ApiErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação nos dados de entrada",
                "Um ou mais campos estão inválidos. Verifique os detalhes.",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            Exception ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Requisição inválida",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno do servidor",
                ex.getMessage() != null ? ex.getMessage() : "Ocorreu um erro inesperado.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
