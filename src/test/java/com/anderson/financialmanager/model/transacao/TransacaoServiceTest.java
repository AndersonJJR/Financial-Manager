package com.anderson.financialmanager.model.transacao;

import com.anderson.financialmanager.config.SecurityContextHelper;
import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.categoria.Categoria;
import com.anderson.financialmanager.model.categoria.CategoriaService;
import com.anderson.financialmanager.model.conta.Conta;
import com.anderson.financialmanager.model.conta.ContaService;
import com.anderson.financialmanager.model.conta.TipoConta;
import com.anderson.financialmanager.model.transacao.dto.*;
import com.anderson.financialmanager.model.transacao.validacao.*;
import com.anderson.financialmanager.model.usuario.RoleUsuario;
import com.anderson.financialmanager.model.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private ContaService contaService;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private SecurityContextHelper securityContextHelper;

    private TransacaoService transacaoService;
    private ExportacaoExtratoService exportacaoExtratoService;

    private Usuario usuarioPadrao;
    private Conta contaPadrao;
    private Categoria categoriaDespesa;
    private Categoria categoriaReceita;

    @BeforeEach
    void setUp() {
        List<ValidadorTransacao> validadoresTransacao = List.of(
                new ValidadorPropriedadeEStatusContaCategoria(),
                new ValidadorCompatibilidadeTipoCategoria(),
                new ValidadorConsistenciaStatusPagamento()
        );
        List<ValidadorDespesaParcelada> validadoresParcela = List.of(
                new ValidadorValorMinimoParcelamento()
        );

        transacaoService = new TransacaoService(
                transacaoRepository,
                contaService,
                categoriaService,
                securityContextHelper,
                validadoresTransacao,
                validadoresParcela
        );

        exportacaoExtratoService = new ExportacaoExtratoService(
                transacaoRepository,
                contaService,
                securityContextHelper
        );

        usuarioPadrao = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Anderson")
                .email("anderson@email.com")
                .role(RoleUsuario.ROLE_USER)
                .ativo(true)
                .build();

        contaPadrao = Conta.builder()
                .id(UUID.randomUUID())
                .nome("Conta Principal")
                .instituicao("Nubank")
                .tipo(TipoConta.CONTA_CORRENTE)
                .saldoInicial(new BigDecimal("1000.00"))
                .ativa(true)
                .usuario(usuarioPadrao)
                .build();

        categoriaDespesa = Categoria.builder()
                .id(UUID.randomUUID())
                .nome("Eletrônicos")
                .corHex("#FF5733")
                .tipo(TipoTransacao.DESPESA)
                .ativa(true)
                .usuario(usuarioPadrao)
                .build();

        categoriaReceita = Categoria.builder()
                .id(UUID.randomUUID())
                .nome("Salário")
                .corHex("#28A745")
                .tipo(TipoTransacao.RECEITA)
                .ativa(true)
                .usuario(usuarioPadrao)
                .build();
    }

    @Test
    @DisplayName("Deve gerar parcelas com ajuste exato de centavos quando informado VALOR_TOTAL")
    void deveGerarParcelasComAjusteDeCentavosNoValorTotal() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(contaService.buscarEntidadeDoUsuario(contaPadrao.getId(), usuarioPadrao.getId())).thenReturn(contaPadrao);
        when(categoriaService.buscarEntidadeDoUsuario(categoriaDespesa.getId(), usuarioPadrao.getId())).thenReturn(categoriaDespesa);
        when(transacaoRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Transacao> lista = invocation.getArgument(0);
            lista.forEach(t -> t.setId(UUID.randomUUID()));
            return lista;
        });

        DespesaParceladaRequest request = new DespesaParceladaRequest(
                "Notebook Dell",
                new BigDecimal("100.00"),
                ModoValorParcelamento.VALOR_TOTAL,
                3,
                Periodicidade.MENSAL,
                LocalDate.of(2026, 10, 10),
                true,
                contaPadrao.getId(),
                categoriaDespesa.getId(),
                "Compra em 3x sem juros"
        );

        ParcelamentoResponse response = transacaoService.criarDespesaParcelada(request);

        assertNotNull(response.grupoParcelamentoId());
        assertEquals(3, response.totalLancamentos());
        assertEquals(new BigDecimal("100.00"), response.valorTotalConsolidado());
        assertEquals(new BigDecimal("33.34"), response.lancamentos().get(0).valor());
        assertEquals(StatusTransacao.EFETIVADA, response.lancamentos().get(0).status());
        assertEquals("1/3", response.lancamentos().get(0).referenciaParcela());

        assertEquals(new BigDecimal("33.33"), response.lancamentos().get(1).valor());
        assertEquals(StatusTransacao.PENDENTE, response.lancamentos().get(1).status());
        assertEquals(LocalDate.of(2026, 11, 10), response.lancamentos().get(1).dataCompetencia());

        assertEquals(new BigDecimal("33.33"), response.lancamentos().get(2).valor());
        assertEquals("3/3", response.lancamentos().get(2).referenciaParcela());
    }

    @Test
    @DisplayName("Deve rejeitar transação de DESPESA vinculada a categoria de RECEITA via Strategy")
    void deveRejeitarTransacaoComCategoriaIncompativel() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(contaService.buscarEntidadeDoUsuario(contaPadrao.getId(), usuarioPadrao.getId())).thenReturn(contaPadrao);
        when(categoriaService.buscarEntidadeDoUsuario(categoriaReceita.getId(), usuarioPadrao.getId())).thenReturn(categoriaReceita);

        TransacaoRequest request = new TransacaoRequest(
                "Supermercado",
                new BigDecimal("250.00"),
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 15),
                TipoTransacao.DESPESA,
                StatusTransacao.EFETIVADA,
                contaPadrao.getId(),
                categoriaReceita.getId(),
                null
        );

        assertThrows(RegraDeNegocioException.class, () -> transacaoService.criar(request));
    }

    @Test
    @DisplayName("Deve calcular corretamente o resumo mensal e percentuais por categoria")
    void deveCalcularResumoMensalCorretamente() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);

        Transacao receita = Transacao.builder()
                .id(UUID.randomUUID())
                .descricao("Salário Outubro")
                .valor(new BigDecimal("5000.00"))
                .dataCompetencia(LocalDate.of(2026, 10, 5))
                .dataPagamento(LocalDate.of(2026, 10, 5))
                .tipo(TipoTransacao.RECEITA)
                .status(StatusTransacao.EFETIVADA)
                .modalidade(ModalidadeTransacao.UNICA)
                .conta(contaPadrao)
                .categoria(categoriaReceita)
                .usuario(usuarioPadrao)
                .build();

        Transacao despesaPaga = Transacao.builder()
                .id(UUID.randomUUID())
                .descricao("Monitor")
                .valor(new BigDecimal("1500.00"))
                .dataCompetencia(LocalDate.of(2026, 10, 12))
                .dataPagamento(LocalDate.of(2026, 10, 12))
                .tipo(TipoTransacao.DESPESA)
                .status(StatusTransacao.EFETIVADA)
                .modalidade(ModalidadeTransacao.UNICA)
                .conta(contaPadrao)
                .categoria(categoriaDespesa)
                .usuario(usuarioPadrao)
                .build();

        Transacao despesaPrevista = Transacao.builder()
                .id(UUID.randomUUID())
                .descricao("Teclado Mecânico")
                .valor(new BigDecimal("500.00"))
                .dataCompetencia(LocalDate.of(2026, 10, 25))
                .tipo(TipoTransacao.DESPESA)
                .status(StatusTransacao.PENDENTE)
                .modalidade(ModalidadeTransacao.PARCELADA)
                .parcelaAtual(1)
                .totalParcelas(2)
                .conta(contaPadrao)
                .categoria(categoriaDespesa)
                .usuario(usuarioPadrao)
                .build();

        when(transacaoRepository.findAllByUsuarioIdAndPeriodo(
                usuarioPadrao.getId(),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)
        )).thenReturn(List.of(receita, despesaPaga, despesaPrevista));

        ResumoMensalResponse resumo = transacaoService.obterResumoMensal(2026, 10);

        assertEquals(new BigDecimal("5000.00"), resumo.totalEntradasRealizadas());
        assertEquals(new BigDecimal("1500.00"), resumo.totalSaidasPagas());
        assertEquals(new BigDecimal("500.00"), resumo.totalSaidasPrevistas());
        assertEquals(new BigDecimal("2000.00"), resumo.totalSaidasConsolidado());
        assertEquals(new BigDecimal("3500.00"), resumo.saldoLiquidoRealizado());
        assertEquals(new BigDecimal("3000.00"), resumo.saldoLiquidoPrevisto());
        assertEquals(1, resumo.gastosPorCategoria().size());
        assertEquals(new BigDecimal("100.00"), resumo.gastosPorCategoria().getFirst().percentual());
    }

    @Test
    @DisplayName("Deve exportar extrato em CSV e JSON pretty-printed")
    void deveExportarExtratoEmCsvEJson() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);

        Transacao despesa = Transacao.builder()
                .id(UUID.randomUUID())
                .descricao("Assinatura Cloud")
                .valor(new BigDecimal("89.90"))
                .dataCompetencia(LocalDate.of(2026, 10, 10))
                .dataPagamento(LocalDate.of(2026, 10, 10))
                .tipo(TipoTransacao.DESPESA)
                .status(StatusTransacao.EFETIVADA)
                .modalidade(ModalidadeTransacao.RECORRENTE)
                .parcelaAtual(1)
                .totalParcelas(12)
                .conta(contaPadrao)
                .categoria(categoriaDespesa)
                .usuario(usuarioPadrao)
                .observacao("Plano anual")
                .build();

        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fim = LocalDate.of(2026, 10, 31);

        when(transacaoRepository.findAllByUsuarioIdAndPeriodo(usuarioPadrao.getId(), inicio, fim))
                .thenReturn(List.of(despesa));

        byte[] csvBytes = exportacaoExtratoService.exportarExtrato(inicio, fim, null, FormatoExportacao.CSV);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("ID;Data Competencia;Data Pagamento;Descricao;Tipo;Status;Modalidade;Parcela;Valor Numerico;Valor Formatado (BRL);Conta;Instituicao;Categoria;Observacao"));
        assertTrue(csvContent.contains("Assinatura Cloud"));
        assertTrue(csvContent.contains("1/12"));

        byte[] jsonBytes = exportacaoExtratoService.exportarExtrato(inicio, fim, null, FormatoExportacao.JSON);
        String jsonContent = new String(jsonBytes, StandardCharsets.UTF_8);
        assertTrue(jsonContent.contains("\"descricao\" : \"Assinatura Cloud\""));
        assertTrue(jsonContent.contains("\"totalDespesas\" : 89.90"));
    }

    @Test
    @DisplayName("Deve rejeitar efetivação de uma transação cancelada")
    void deveRejeitarEfetivacaoDeTransacaoCancelada() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);

        Transacao cancelada = Transacao.builder()
                .id(UUID.randomUUID())
                .descricao("Compra cancelada")
                .valor(new BigDecimal("100.00"))
                .dataCompetencia(LocalDate.now())
                .tipo(TipoTransacao.DESPESA)
                .status(StatusTransacao.CANCELADA)
                .conta(contaPadrao)
                .categoria(categoriaDespesa)
                .usuario(usuarioPadrao)
                .build();

        when(transacaoRepository.findByIdAndUsuarioIdWithDetails(cancelada.getId(), usuarioPadrao.getId()))
                .thenReturn(java.util.Optional.of(cancelada));

        assertThrows(RegraDeNegocioException.class, () -> transacaoService.efetivar(cancelada.getId(), LocalDate.now()));
        verify(transacaoRepository, never()).save(any(Transacao.class));
    }

    @Test
    @DisplayName("Deve garantir que transação PENDENTE tenha data de pagamento nula mesmo se enviada no request")
    void deveGarantirDataPagamentoNulaEmTransacaoPendente() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(contaService.buscarEntidadeDoUsuario(contaPadrao.getId(), usuarioPadrao.getId())).thenReturn(contaPadrao);
        when(categoriaService.buscarEntidadeDoUsuario(categoriaDespesa.getId(), usuarioPadrao.getId())).thenReturn(categoriaDespesa);
        when(transacaoRepository.save(any(Transacao.class))).thenAnswer(i -> i.getArgument(0));

        TransacaoRequest request = new TransacaoRequest(
                "Fatura Futura",
                new BigDecimal("200.00"),
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 10), // enviou data de pagamento
                TipoTransacao.DESPESA,
                StatusTransacao.PENDENTE, // mas status é PENDENTE
                contaPadrao.getId(),
                categoriaDespesa.getId(),
                null
        );

        TransacaoResponse response = transacaoService.criar(request);

        assertEquals(StatusTransacao.PENDENTE, response.status());
        assertNull(response.dataPagamento());
    }
}
