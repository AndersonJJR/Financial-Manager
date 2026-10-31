package com.anderson.financialmanager.model.transacao.dto;

import com.anderson.financialmanager.model.transacao.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransacaoResponse(
        UUID id,
        String descricao,
        BigDecimal valor,
        LocalDate dataCompetencia,
        LocalDate dataPagamento,
        TipoTransacao tipo,
        StatusTransacao status,
        ModalidadeTransacao modalidade,
        UUID grupoParcelamentoId,
        Integer parcelaAtual,
        Integer totalParcelas,
        String referenciaParcela,
        Periodicidade periodicidade,
        Boolean recorrenciaAtiva,
        String observacao,
        UUID contaId,
        String contaNome,
        UUID categoriaId,
        String categoriaNome,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static TransacaoResponse fromEntity(Transacao transacao) {
        return new TransacaoResponse(
                transacao.getId(),
                transacao.getDescricao(),
                transacao.getValor(),
                transacao.getDataCompetencia(),
                transacao.getDataPagamento(),
                transacao.getTipo(),
                transacao.getStatus(),
                transacao.getModalidade(),
                transacao.getGrupoParcelamentoId(),
                transacao.getParcelaAtual(),
                transacao.getTotalParcelas(),
                transacao.getReferenciaParcela(),
                transacao.getPeriodicidade(),
                transacao.getRecorrenciaAtiva(),
                transacao.getObservacao(),
                transacao.getConta().getId(),
                transacao.getConta().getNome(),
                transacao.getCategoria().getId(),
                transacao.getCategoria().getNome(),
                transacao.getCriadoEm(),
                transacao.getAtualizadoEm()
        );
    }
}
