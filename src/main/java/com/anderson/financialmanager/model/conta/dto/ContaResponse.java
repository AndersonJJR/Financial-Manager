package com.anderson.financialmanager.model.conta.dto;

import com.anderson.financialmanager.model.conta.Conta;
import com.anderson.financialmanager.model.conta.TipoConta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContaResponse(
        UUID id,
        String nome,
        String instituicao,
        TipoConta tipo,
        BigDecimal saldoInicial,
        BigDecimal saldoAtual,
        Boolean ativa,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static ContaResponse fromEntity(Conta conta) {
        return fromEntity(conta, conta.getSaldoInicial());
    }

    public static ContaResponse fromEntity(Conta conta, BigDecimal saldoAtual) {
        return new ContaResponse(
                conta.getId(),
                conta.getNome(),
                conta.getInstituicao(),
                conta.getTipo(),
                conta.getSaldoInicial(),
                saldoAtual != null ? saldoAtual : conta.getSaldoInicial(),
                conta.getAtiva(),
                conta.getCriadoEm(),
                conta.getAtualizadoEm()
        );
    }
}
