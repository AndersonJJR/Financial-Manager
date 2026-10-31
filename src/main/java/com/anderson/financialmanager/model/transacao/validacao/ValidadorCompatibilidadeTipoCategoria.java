package com.anderson.financialmanager.model.transacao.validacao;

import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.categoria.Categoria;
import com.anderson.financialmanager.model.transacao.Transacao;
import org.springframework.stereotype.Component;

@Component
public class ValidadorCompatibilidadeTipoCategoria implements ValidadorTransacao {

    @Override
    public void validar(Transacao transacao) {
        Categoria categoria = transacao.getCategoria();
        if (categoria != null && transacao.getTipo() != null && categoria.getTipo() != transacao.getTipo()) {
            throw new RegraDeNegocioException(
                    "Incompatibilidade de tipo: a transação é do tipo %s, mas a categoria '%s' é do tipo %s."
                            .formatted(transacao.getTipo(), categoria.getNome(), categoria.getTipo())
            );
        }
    }
}
