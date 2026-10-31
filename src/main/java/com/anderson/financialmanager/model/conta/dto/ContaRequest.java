package com.anderson.financialmanager.model.conta.dto;

import com.anderson.financialmanager.model.conta.TipoConta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ContaRequest(
        @NotBlank(message = "O nome da conta é obrigatório")
        @Size(min = 2, max = 100, message = "O nome da conta deve ter entre 2 e 100 caracteres")
        String nome,

        @Size(max = 100, message = "A instituição deve ter no máximo 100 caracteres")
        String instituicao,

        @NotNull(message = "O tipo da conta é obrigatório")
        TipoConta tipo,

        BigDecimal saldoInicial,

        Boolean ativa
) {
}
