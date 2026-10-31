package com.anderson.financialmanager.model.transacao.dto;

import com.anderson.financialmanager.model.transacao.StatusTransacao;
import com.anderson.financialmanager.model.transacao.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoRequest(
        @NotBlank(message = "A descrição é obrigatória")
        @Size(min = 2, max = 150, message = "A descrição deve ter entre 2 e 150 caracteres")
        String descricao,

        @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor da transação deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "A data de competência/vencimento é obrigatória")
        LocalDate dataCompetencia,

        LocalDate dataPagamento,

        @NotNull(message = "O tipo da transação (RECEITA ou DESPESA) é obrigatório")
        TipoTransacao tipo,

        StatusTransacao status,

        @NotNull(message = "O identificador da conta é obrigatório")
        UUID contaId,

        @NotNull(message = "O identificador da categoria é obrigatório")
        UUID categoriaId,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String observacao
) {
}
