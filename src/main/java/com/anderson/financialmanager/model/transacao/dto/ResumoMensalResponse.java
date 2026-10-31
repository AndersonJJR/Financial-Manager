package com.anderson.financialmanager.model.transacao.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoMensalResponse(
        int ano,
        int mes,
        BigDecimal totalEntradasRealizadas,
        BigDecimal totalEntradasPrevistas,
        BigDecimal totalSaidasPagas,
        BigDecimal totalSaidasPrevistas,
        BigDecimal totalSaidasConsolidado,
        BigDecimal saldoLiquidoRealizado,
        BigDecimal saldoLiquidoPrevisto,
        List<GastoPorCategoriaDto> gastosPorCategoria
) {
}
