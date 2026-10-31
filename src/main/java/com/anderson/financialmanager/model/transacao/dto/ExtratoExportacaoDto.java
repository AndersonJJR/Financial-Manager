package com.anderson.financialmanager.model.transacao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExtratoExportacaoDto(
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalDateTime geradoEm,
        int totalRegistros,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldoPeriodo,
        List<ItemExtratoDto> transacoes
) {
    public record ItemExtratoDto(
            UUID id,
            LocalDate dataCompetencia,
            LocalDate dataPagamento,
            String descricao,
            String tipo,
            String status,
            String modalidade,
            String parcela,
            BigDecimal valor,
            String valorFormatado,
            String conta,
            String instituicaoConta,
            String categoria,
            String observacao
    ) {
    }
}
