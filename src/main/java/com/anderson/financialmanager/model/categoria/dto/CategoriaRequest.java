package com.anderson.financialmanager.model.categoria.dto;

import com.anderson.financialmanager.model.transacao.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(min = 2, max = 100, message = "O nome da categoria deve ter entre 2 e 100 caracteres")
        String nome,

        @Size(max = 250, message = "A descrição deve ter no máximo 250 caracteres")
        String descricao,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "A cor deve estar no formato hexadecimal (#RRGGBB)")
        String corHex,

        @NotNull(message = "O tipo da categoria (RECEITA ou DESPESA) é obrigatório")
        TipoTransacao tipo,

        Boolean ativa
) {
}
