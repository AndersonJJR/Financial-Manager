package com.anderson.financialmanager.model.transacao.validacao;

import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.transacao.ModoValorParcelamento;
import com.anderson.financialmanager.model.transacao.dto.DespesaParceladaRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ValidadorValorMinimoParcelamento implements ValidadorDespesaParcelada {

    private static final BigDecimal VALOR_MINIMO_PARCELA = new BigDecimal("0.01");

    @Override
    public void validar(DespesaParceladaRequest request) {
        if (request.totalParcelas() == null || request.totalParcelas() < 2) {
            throw new RegraDeNegocioException("Uma despesa parcelada deve ter pelo menos 2 parcelas.");
        }

        if (request.valorInformado() == null || request.valorInformado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("O valor informado para o parcelamento deve ser maior que zero.");
        }

        if (request.modoValor() == ModoValorParcelamento.VALOR_TOTAL) {
            BigDecimal valorBaseParcela = request.valorInformado()
                    .divide(BigDecimal.valueOf(request.totalParcelas()), 2, RoundingMode.DOWN);
            if (valorBaseParcela.compareTo(VALOR_MINIMO_PARCELA) < 0) {
                throw new RegraDeNegocioException(
                        "O valor total dividido pelo número de parcelas resulta em parcelas menores que R$ 0,01."
                );
            }
        } else if (request.valorInformado().compareTo(VALOR_MINIMO_PARCELA) < 0) {
            throw new RegraDeNegocioException("O valor de cada parcela deve ser de no mínimo R$ 0,01.");
        }
    }
}
