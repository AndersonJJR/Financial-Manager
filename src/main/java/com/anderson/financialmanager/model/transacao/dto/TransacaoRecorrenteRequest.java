package com.anderson.financialmanager.model.transacao.dto;

import com.anderson.financialmanager.model.transacao.Periodicidade;
import com.anderson.financialmanager.model.transacao.TipoTransacao;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoRecorrenteRequest(
        @NotBlank(message = "A descrição da transação recorrente é obrigatória")
        @Size(min = 2, max = 150, message = "A descrição deve ter entre 2 e 150 caracteres")
        String descricao,

        @NotNull(message = "O valor fixo/recorrente é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "O tipo da transação (RECEITA ou DESPESA) é obrigatório")
        TipoTransacao tipo,

        @NotNull(message = "A periodicidade da recorrência é obrigatória")
        Periodicidade periodicidade,

        @NotNull(message = "A data de início da recorrência é obrigatória")
        LocalDate dataInicio,

        @NotNull(message = "A quantidade de ocorrências a projetar é obrigatória")
        @Min(value = 1, message = "Deve gerar no mínimo 1 ocorrência")
        @Max(value = 60, message = "O limite de projeção por lote é de 60 ocorrências")
        Integer quantidadeOcorrencias,

        Boolean primeiraOcorrenciaEfetivada,

        @NotNull(message = "O identificador da conta é obrigatório")
        UUID contaId,

        @NotNull(message = "O identificador da categoria é obrigatório")
        UUID categoriaId,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String observacao
) {
}
