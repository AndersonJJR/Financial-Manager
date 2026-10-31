package com.anderson.financialmanager.model.transacao;

import com.anderson.financialmanager.config.SecurityContextHelper;
import com.anderson.financialmanager.exception.RecursoNaoEncontradoException;
import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.categoria.Categoria;
import com.anderson.financialmanager.model.categoria.CategoriaService;
import com.anderson.financialmanager.model.conta.Conta;
import com.anderson.financialmanager.model.conta.ContaService;
import com.anderson.financialmanager.model.transacao.dto.*;
import com.anderson.financialmanager.model.transacao.validacao.ValidadorDespesaParcelada;
import com.anderson.financialmanager.model.transacao.validacao.ValidadorTransacao;
import com.anderson.financialmanager.model.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final ContaService contaService;
    private final CategoriaService categoriaService;
    private final SecurityContextHelper securityContextHelper;
    private final List<ValidadorTransacao> validadoresTransacao;
    private final List<ValidadorDespesaParcelada> validadoresDespesaParcelada;

    @Transactional
    public TransacaoResponse criar(TransacaoRequest request) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Conta conta = contaService.buscarEntidadeDoUsuario(request.contaId(), usuario.getId());
        Categoria categoria = categoriaService.buscarEntidadeDoUsuario(request.categoriaId(), usuario.getId());

        StatusTransacao status = definirStatus(request.status(), request.dataPagamento());
        LocalDate dataPagamento = resolverDataPagamento(status, request.dataPagamento(), request.dataCompetencia());

        Transacao transacao = Transacao.builder()
                .descricao(request.descricao().trim())
                .valor(request.valor().setScale(2, RoundingMode.HALF_UP))
                .dataCompetencia(request.dataCompetencia())
                .dataPagamento(dataPagamento)
                .tipo(request.tipo())
                .status(status)
                .modalidade(ModalidadeTransacao.UNICA)
                .recorrenciaAtiva(false)
                .observacao(request.observacao() != null ? request.observacao().trim() : null)
                .conta(conta)
                .categoria(categoria)
                .usuario(usuario)
                .build();

        executarValidadoresTransacao(transacao);

        return TransacaoResponse.fromEntity(transacaoRepository.save(transacao));
    }

    @Transactional
    public ParcelamentoResponse criarDespesaParcelada(DespesaParceladaRequest request) {
        validadoresDespesaParcelada.forEach(validador -> validador.validar(request));

        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Conta conta = contaService.buscarEntidadeDoUsuario(request.contaId(), usuario.getId());
        Categoria categoria = categoriaService.buscarEntidadeDoUsuario(request.categoriaId(), usuario.getId());

        int totalParcelas = request.totalParcelas();
        BigDecimal valorInformado = request.valorInformado().setScale(2, RoundingMode.HALF_UP);

        BigDecimal valorParcelaPadrao;
        BigDecimal valorPrimeiraParcela;
        BigDecimal valorTotalConsolidado;

        if (request.modoValor() == ModoValorParcelamento.VALOR_TOTAL) {
            valorTotalConsolidado = valorInformado;
            valorParcelaPadrao = valorInformado.divide(BigDecimal.valueOf(totalParcelas), 2, RoundingMode.DOWN);
            BigDecimal somaParcelasPadrao = valorParcelaPadrao.multiply(BigDecimal.valueOf(totalParcelas));
            BigDecimal diferencaCentavos = valorInformado.subtract(somaParcelasPadrao);
            valorPrimeiraParcela = valorParcelaPadrao.add(diferencaCentavos);
        } else {
            valorParcelaPadrao = valorInformado;
            valorPrimeiraParcela = valorInformado;
            valorTotalConsolidado = valorInformado.multiply(BigDecimal.valueOf(totalParcelas));
        }

        UUID grupoId = UUID.randomUUID();
        List<Transacao> parcelas = new ArrayList<>(totalParcelas);
        String descricaoBase = request.descricao().trim();

        for (int i = 0; i < totalParcelas; i++) {
            int numeroParcela = i + 1;
            LocalDate dataVencimento = request.periodicidade().avancarData(request.dataPrimeiraParcela(), i);
            BigDecimal valorParcela = (i == 0) ? valorPrimeiraParcela : valorParcelaPadrao;

            boolean isPaga = (i == 0) && Boolean.TRUE.equals(request.primeiraParcelaPaga());
            StatusTransacao status = isPaga ? StatusTransacao.EFETIVADA : StatusTransacao.PENDENTE;
            LocalDate dataPagamento = isPaga ? dataVencimento : null;

            Transacao parcela = Transacao.builder()
                    .descricao("%s (%d/%d)".formatted(descricaoBase, numeroParcela, totalParcelas))
                    .valor(valorParcela)
                    .dataCompetencia(dataVencimento)
                    .dataPagamento(dataPagamento)
                    .tipo(TipoTransacao.DESPESA)
                    .status(status)
                    .modalidade(ModalidadeTransacao.PARCELADA)
                    .grupoParcelamentoId(grupoId)
                    .parcelaAtual(numeroParcela)
                    .totalParcelas(totalParcelas)
                    .periodicidade(request.periodicidade())
                    .recorrenciaAtiva(false)
                    .observacao(request.observacao() != null ? request.observacao().trim() : null)
                    .conta(conta)
                    .categoria(categoria)
                    .usuario(usuario)
                    .build();

            executarValidadoresTransacao(parcela);
            parcelas.add(parcela);
        }

        List<TransacaoResponse> salvas = transacaoRepository.saveAll(parcelas)
                .stream()
                .map(TransacaoResponse::fromEntity)
                .toList();

        return new ParcelamentoResponse(
                grupoId,
                descricaoBase,
                ModalidadeTransacao.PARCELADA,
                request.periodicidade(),
                totalParcelas,
                valorTotalConsolidado,
                salvas
        );
    }

    @Transactional
    public ParcelamentoResponse criarTransacaoRecorrente(TransacaoRecorrenteRequest request) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Conta conta = contaService.buscarEntidadeDoUsuario(request.contaId(), usuario.getId());
        Categoria categoria = categoriaService.buscarEntidadeDoUsuario(request.categoriaId(), usuario.getId());

        int totalOcorrencias = request.quantidadeOcorrencias();
        BigDecimal valorUnitario = request.valor().setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorTotal = valorUnitario.multiply(BigDecimal.valueOf(totalOcorrencias));
        UUID grupoRecorrenciaId = UUID.randomUUID();
        String descricaoBase = request.descricao().trim();

        List<Transacao> ocorrencias = new ArrayList<>(totalOcorrencias);

        for (int i = 0; i < totalOcorrencias; i++) {
            int numeroOcorrencia = i + 1;
            LocalDate dataCompetencia = request.periodicidade().avancarData(request.dataInicio(), i);
            boolean efetivada = (i == 0) && Boolean.TRUE.equals(request.primeiraOcorrenciaEfetivada());
            StatusTransacao status = efetivada ? StatusTransacao.EFETIVADA : StatusTransacao.PENDENTE;
            LocalDate dataPagamento = efetivada ? dataCompetencia : null;

            Transacao ocorrencia = Transacao.builder()
                    .descricao(descricaoBase)
                    .valor(valorUnitario)
                    .dataCompetencia(dataCompetencia)
                    .dataPagamento(dataPagamento)
                    .tipo(request.tipo())
                    .status(status)
                    .modalidade(ModalidadeTransacao.RECORRENTE)
                    .grupoParcelamentoId(grupoRecorrenciaId)
                    .parcelaAtual(numeroOcorrencia)
                    .totalParcelas(totalOcorrencias)
                    .periodicidade(request.periodicidade())
                    .recorrenciaAtiva(true)
                    .observacao(request.observacao() != null ? request.observacao().trim() : null)
                    .conta(conta)
                    .categoria(categoria)
                    .usuario(usuario)
                    .build();

            executarValidadoresTransacao(ocorrencia);
            ocorrencias.add(ocorrencia);
        }

        List<TransacaoResponse> salvas = transacaoRepository.saveAll(ocorrencias)
                .stream()
                .map(TransacaoResponse::fromEntity)
                .toList();

        return new ParcelamentoResponse(
                grupoRecorrenciaId,
                descricaoBase,
                ModalidadeTransacao.RECORRENTE,
                request.periodicidade(),
                totalOcorrencias,
                valorTotal,
                salvas
        );
    }

    @Transactional
    public List<TransacaoResponse> cancelarRecorrenciaOuParcelasFuturas(UUID grupoId) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        List<Transacao> lancamentos = transacaoRepository.findAllByUsuarioIdAndGrupoParcelamentoId(usuario.getId(), grupoId);

        if (lancamentos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhum grupo de parcelamento ou recorrência encontrado para o identificador informado.");
        }

        for (Transacao transacao : lancamentos) {
            transacao.setRecorrenciaAtiva(false);
            if (transacao.getStatus() == StatusTransacao.PENDENTE) {
                transacao.setStatus(StatusTransacao.CANCELADA);
                transacao.setDataPagamento(null);
            }
        }

        return transacaoRepository.saveAll(lancamentos)
                .stream()
                .map(TransacaoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransacaoResponse> listarPorGrupo(UUID grupoId) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        List<Transacao> lancamentos = transacaoRepository.findAllByUsuarioIdAndGrupoParcelamentoId(usuario.getId(), grupoId);

        if (lancamentos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhum grupo de parcelamento ou recorrência encontrado para o identificador informado.");
        }

        return lancamentos.stream()
                .map(TransacaoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransacaoResponse> listar(
            LocalDate dataInicio,
            LocalDate dataFim,
            TipoTransacao tipo,
            StatusTransacao status,
            UUID contaId,
            UUID categoriaId
    ) {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new RegraDeNegocioException("A data inicial não pode ser posterior à data final.");
        }

        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        return transacaoRepository.buscarComFiltros(
                        usuario.getId(),
                        dataInicio,
                        dataFim,
                        tipo,
                        status,
                        contaId,
                        categoriaId
                )
                .stream()
                .map(TransacaoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransacaoResponse buscarPorId(UUID id) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Transacao transacao = buscarEntidadeDoUsuario(id, usuario.getId());
        return TransacaoResponse.fromEntity(transacao);
    }

    @Transactional
    public TransacaoResponse atualizar(UUID id, TransacaoRequest request) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Transacao transacao = buscarEntidadeDoUsuario(id, usuario.getId());
        Conta conta = contaService.buscarEntidadeDoUsuario(request.contaId(), usuario.getId());
        Categoria categoria = categoriaService.buscarEntidadeDoUsuario(request.categoriaId(), usuario.getId());

        StatusTransacao status = definirStatus(request.status(), request.dataPagamento());
        LocalDate dataPagamento = resolverDataPagamento(status, request.dataPagamento(), request.dataCompetencia());

        transacao.setDescricao(request.descricao().trim());
        transacao.setValor(request.valor().setScale(2, RoundingMode.HALF_UP));
        transacao.setDataCompetencia(request.dataCompetencia());
        transacao.setDataPagamento(dataPagamento);
        transacao.setTipo(request.tipo());
        transacao.setStatus(status);
        transacao.setObservacao(request.observacao() != null ? request.observacao().trim() : null);
        transacao.setConta(conta);
        transacao.setCategoria(categoria);

        executarValidadoresTransacao(transacao);

        return TransacaoResponse.fromEntity(transacaoRepository.save(transacao));
    }

    @Transactional
    public TransacaoResponse efetivar(UUID id, LocalDate dataPagamento) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Transacao transacao = buscarEntidadeDoUsuario(id, usuario.getId());

        transacao.setStatus(StatusTransacao.EFETIVADA);
        transacao.setDataPagamento(dataPagamento != null ? dataPagamento : LocalDate.now());

        executarValidadoresTransacao(transacao);

        return TransacaoResponse.fromEntity(transacaoRepository.save(transacao));
    }

    @Transactional
    public void excluir(UUID id) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Transacao transacao = buscarEntidadeDoUsuario(id, usuario.getId());
        transacaoRepository.delete(transacao);
    }

    @Transactional(readOnly = true)
    public ResumoMensalResponse obterResumoMensal(int ano, int mes) {
        if (mes < 1 || mes > 12) {
            throw new RegraDeNegocioException("O mês de referência deve estar entre 1 e 12.");
        }
        if (ano < 1970 || ano > 2100) {
            throw new RegraDeNegocioException("O ano de referência deve estar entre 1970 e 2100.");
        }

        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        YearMonth competencia = YearMonth.of(ano, mes);
        LocalDate dataInicio = competencia.atDay(1);
        LocalDate dataFim = competencia.atEndOfMonth();

        List<Transacao> transacoesMes = transacaoRepository.findAllByUsuarioIdAndPeriodo(
                usuario.getId(),
                dataInicio,
                dataFim
        );

        List<Transacao> ativas = transacoesMes.stream()
                .filter(t -> t.getStatus() != StatusTransacao.CANCELADA)
                .toList();

        BigDecimal totalEntradasRealizadas = somarValores(ativas, TipoTransacao.RECEITA, StatusTransacao.EFETIVADA);
        BigDecimal totalEntradasPendentes = somarValores(ativas, TipoTransacao.RECEITA, StatusTransacao.PENDENTE);
        BigDecimal totalEntradasPrevistas = totalEntradasRealizadas.add(totalEntradasPendentes);

        BigDecimal totalSaidasPagas = somarValores(ativas, TipoTransacao.DESPESA, StatusTransacao.EFETIVADA);
        BigDecimal totalSaidasPrevistas = somarValores(ativas, TipoTransacao.DESPESA, StatusTransacao.PENDENTE);
        BigDecimal totalSaidasConsolidado = totalSaidasPagas.add(totalSaidasPrevistas);

        BigDecimal saldoLiquidoRealizado = totalEntradasRealizadas.subtract(totalSaidasPagas);
        BigDecimal saldoLiquidoPrevisto = totalEntradasPrevistas.subtract(totalSaidasConsolidado);

        Map<UUID, List<Transacao>> despesasPorCategoria = ativas.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DESPESA)
                .collect(Collectors.groupingBy(t -> t.getCategoria().getId()));

        List<GastoPorCategoriaDto> detalhamentoCategorias = despesasPorCategoria.values()
                .stream()
                .map(lista -> {
                    Categoria cat = lista.getFirst().getCategoria();
                    BigDecimal somaCategoria = lista.stream()
                            .map(Transacao::getValor)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal percentual = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    if (totalSaidasConsolidado.compareTo(BigDecimal.ZERO) > 0) {
                        percentual = somaCategoria
                                .multiply(new BigDecimal("100"))
                                .divide(totalSaidasConsolidado, 2, RoundingMode.HALF_UP);
                    }

                    return new GastoPorCategoriaDto(
                            cat.getId(),
                            cat.getNome(),
                            cat.getCorHex(),
                            somaCategoria,
                            percentual,
                            lista.size()
                    );
                })
                .sorted(Comparator.comparing(GastoPorCategoriaDto::valorNominal).reversed())
                .toList();

        return new ResumoMensalResponse(
                ano,
                mes,
                totalEntradasRealizadas,
                totalEntradasPrevistas,
                totalSaidasPagas,
                totalSaidasPrevistas,
                totalSaidasConsolidado,
                saldoLiquidoRealizado,
                saldoLiquidoPrevisto,
                detalhamentoCategorias
        );
    }

    private BigDecimal somarValores(List<Transacao> transacoes, TipoTransacao tipo, StatusTransacao status) {
        return transacoes.stream()
                .filter(t -> t.getTipo() == tipo && t.getStatus() == status)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private StatusTransacao definirStatus(StatusTransacao statusSolicitado, LocalDate dataPagamento) {
        if (statusSolicitado != null) {
            return statusSolicitado;
        }
        return dataPagamento != null ? StatusTransacao.EFETIVADA : StatusTransacao.PENDENTE;
    }

    private LocalDate resolverDataPagamento(StatusTransacao status, LocalDate dataPagamento, LocalDate dataCompetencia) {
        if (status == StatusTransacao.CANCELADA) {
            return null;
        }
        if (status == StatusTransacao.EFETIVADA && dataPagamento == null) {
            return dataCompetencia;
        }
        return dataPagamento;
    }

    private void executarValidadoresTransacao(Transacao transacao) {
        validadoresTransacao.forEach(validador -> validador.validar(transacao));
    }

    private Transacao buscarEntidadeDoUsuario(UUID id, UUID usuarioId) {
        return transacaoRepository.findByIdAndUsuarioIdWithDetails(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transação não encontrada para o identificador informado."));
    }
}
