package com.anderson.financialmanager.model.transacao.validacao;

import com.anderson.financialmanager.model.transacao.dto.DespesaParceladaRequest;

public interface ValidadorDespesaParcelada {

    void validar(DespesaParceladaRequest request);
}
