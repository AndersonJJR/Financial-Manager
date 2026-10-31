package com.anderson.financialmanager.model.transacao.dto;

import com.anderson.financialmanager.model.transacao.ModalidadeTransacao;
import com.anderson.financialmanager.model.transacao.Periodicidade;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ParcelamentoResponse(
        UUID grupoParcelamentoId,
        String descricaoBase,
        ModalidadeTransacao modalidade,
        Periodicidade periodicidade,
        Integer totalLancamentos,
        BigDecimal valorTotalConsolidado,
        List<TransacaoResponse> lancamentos
) {
}
