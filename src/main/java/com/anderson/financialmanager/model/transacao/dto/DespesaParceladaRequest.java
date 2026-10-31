package com.anderson.financialmanager.model.transacao.dto;

import com.anderson.financialmanager.model.transacao.ModoValorParcelamento;
import com.anderson.financialmanager.model.transacao.Periodicidade;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DespesaParceladaRequest(
        @NotBlank(message = "A descrição da compra parcelada é obrigatória")
        @Size(min = 2, max = 150, message = "A descrição deve ter entre 2 e 150 caracteres")
        String descricao,

        @NotNull(message = "O valor informado é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal valorInformado,

        @NotNull(message = "O modo de cálculo do valor (VALOR_TOTAL ou VALOR_PARCELA) é obrigatório")
        ModoValorParcelamento modoValor,

        @NotNull(message = "O total de parcelas é obrigatório")
        @Min(value = 2, message = "Uma compra parcelada deve possuir no mínimo 2 parcelas")
        @Max(value = 240, message = "O número máximo de parcelas permitido é 240")
        Integer totalParcelas,

        @NotNull(message = "A periodicidade entre as parcelas é obrigatória")
        Periodicidade periodicidade,

        @NotNull(message = "A data da primeira parcela é obrigatória")
        LocalDate dataPrimeiraParcela,

        Boolean primeiraParcelaPaga,

        @NotNull(message = "O identificador da conta é obrigatório")
        UUID contaId,

        @NotNull(message = "O identificador da categoria é obrigatório")
        UUID categoriaId,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String observacao
) {
}
