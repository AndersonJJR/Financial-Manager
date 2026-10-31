package com.anderson.financialmanager.model.transacao.validacao;

import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.transacao.ModalidadeTransacao;
import com.anderson.financialmanager.model.transacao.StatusTransacao;
import com.anderson.financialmanager.model.transacao.Transacao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ValidadorConsistenciaStatusPagamento implements ValidadorTransacao {

    @Override
    public void validar(Transacao transacao) {
        if (transacao.getValor() == null || transacao.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("O valor da transação deve ser estritamente positivo.");
        }

        if (transacao.getStatus() == StatusTransacao.CANCELADA && transacao.getDataPagamento() != null) {
            throw new RegraDeNegocioException("Uma transação cancelada não pode possuir data de pagamento/efetivação.");
        }

        if (transacao.getModalidade() == ModalidadeTransacao.PARCELADA) {
            if (transacao.getParcelaAtual() == null || transacao.getTotalParcelas() == null
                    || transacao.getParcelaAtual() < 1
                    || transacao.getParcelaAtual() > transacao.getTotalParcelas()) {
                throw new RegraDeNegocioException("Numeração de parcelas inválida na transação parcelada.");
            }
        }
    }
}
