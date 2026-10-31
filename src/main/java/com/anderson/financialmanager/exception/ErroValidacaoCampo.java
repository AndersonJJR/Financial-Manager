package com.anderson.financialmanager.exception;

public record ErroValidacaoCampo(
        String campo,
        String mensagem
) {
}
