package com.anderson.financialmanager.model.transacao.validacao;

import com.anderson.financialmanager.exception.AcessoNegadoException;
import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.categoria.Categoria;
import com.anderson.financialmanager.model.conta.Conta;
import com.anderson.financialmanager.model.transacao.Transacao;
import com.anderson.financialmanager.model.usuario.Usuario;
import org.springframework.stereotype.Component;

@Component
public class ValidadorPropriedadeEStatusContaCategoria implements ValidadorTransacao {

    @Override
    public void validar(Transacao transacao) {
        Usuario usuario = transacao.getUsuario();
        Conta conta = transacao.getConta();
        Categoria categoria = transacao.getCategoria();

        if (conta == null || categoria == null || usuario == null) {
            throw new RegraDeNegocioException("Transação deve estar vinculada a um usuário, uma conta e uma categoria.");
        }

        if (!conta.getUsuario().getId().equals(usuario.getId())) {
            throw new AcessoNegadoException("A conta informada não pertence ao usuário autenticado.");
        }

        if (!categoria.getUsuario().getId().equals(usuario.getId())) {
            throw new AcessoNegadoException("A categoria informada não pertence ao usuário autenticado.");
        }

        if (!Boolean.TRUE.equals(conta.getAtiva())) {
            throw new RegraDeNegocioException("Não é permitido registrar transações em uma conta inativa.");
        }

        if (!Boolean.TRUE.equals(categoria.getAtiva())) {
            throw new RegraDeNegocioException("Não é permitido registrar transações em uma categoria inativa.");
        }
    }
}
