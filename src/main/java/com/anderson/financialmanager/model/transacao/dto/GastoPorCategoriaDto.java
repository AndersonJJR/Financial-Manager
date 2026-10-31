package com.anderson.financialmanager.model.transacao.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record GastoPorCategoriaDto(
        UUID categoriaId,
        String categoriaNome,
        String corHex,
        BigDecimal valorNominal,
        BigDecimal percentual,
        long quantidadeTransacoes
) {
}
