package com.anderson.financialmanager.model.categoria.dto;

import com.anderson.financialmanager.model.categoria.Categoria;
import com.anderson.financialmanager.model.transacao.TipoTransacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoriaResponse(
        UUID id,
        String nome,
        String descricao,
        String corHex,
        TipoTransacao tipo,
        Boolean ativa,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static CategoriaResponse fromEntity(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao(),
                categoria.getCorHex(),
                categoria.getTipo(),
                categoria.getAtiva(),
                categoria.getCriadoEm(),
                categoria.getAtualizadoEm()
        );
    }
}
