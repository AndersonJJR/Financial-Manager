package com.anderson.financialmanager.model.conta;

import com.anderson.financialmanager.config.SecurityContextHelper;
import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.conta.dto.ContaRequest;
import com.anderson.financialmanager.model.conta.dto.ContaResponse;
import com.anderson.financialmanager.model.transacao.TransacaoRepository;
import com.anderson.financialmanager.model.usuario.RoleUsuario;
import com.anderson.financialmanager.model.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ContaService contaService;

    private Usuario usuarioPadrao;
    private Conta contaAtiva;
    private Conta contaInativa;

    @BeforeEach
    void setUp() {
        usuarioPadrao = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Anderson")
                .email("anderson@email.com")
                .role(RoleUsuario.ROLE_USER)
                .ativo(true)
                .build();

        contaAtiva = Conta.builder()
                .id(UUID.randomUUID())
                .nome("Nubank")
                .instituicao("Nu Pagamentos")
                .tipo(TipoConta.CONTA_CORRENTE)
                .saldoInicial(new BigDecimal("1000.00"))
                .ativa(true)
                .usuario(usuarioPadrao)
                .build();

        contaInativa = Conta.builder()
                .id(UUID.randomUUID())
                .nome("Inter")
                .instituicao("Banco Inter")
                .tipo(TipoConta.CONTA_CORRENTE)
                .saldoInicial(new BigDecimal("200.00"))
                .ativa(false)
                .usuario(usuarioPadrao)
                .build();
    }

    @Test
    @DisplayName("Deve criar nova conta quando não existir conta com mesmo nome")
    void deveCriarContaComSucesso() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(contaRepository.findByUsuarioIdAndNomeIgnoreCase(usuarioPadrao.getId(), "C6 Bank"))
                .thenReturn(Optional.empty());
        when(contaRepository.save(any(Conta.class))).thenAnswer(i -> {
            Conta c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ContaRequest request = new ContaRequest("C6 Bank", "C6 Bank S.A.", TipoConta.CONTA_CORRENTE, new BigDecimal("500.00"), true);
        ContaResponse response = contaService.criar(request);

        assertNotNull(response);
        assertEquals("C6 Bank", response.nome());
        assertEquals(new BigDecimal("500.00"), response.saldoInicial());
        assertEquals(new BigDecimal("500.00"), response.saldoAtual());
        assertTrue(response.ativa());
    }

    @Test
    @DisplayName("Deve reativar conta inativa existente ao criar conta com mesmo nome")
    void deveReativarContaInativaExistente() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(contaRepository.findByUsuarioIdAndNomeIgnoreCase(usuarioPadrao.getId(), "Inter"))
                .thenReturn(Optional.of(contaInativa));
        when(contaRepository.save(any(Conta.class))).thenAnswer(i -> i.getArgument(0));
        when(transacaoRepository.calcularSaldoTransacoesEfetivadasDaConta(contaInativa.getId(), usuarioPadrao.getId()))
                .thenReturn(new BigDecimal("150.00"));

        ContaRequest request = new ContaRequest("Inter", "Banco Inter S.A.", TipoConta.CONTA_CORRENTE, new BigDecimal("300.00"), true);
        ContaResponse response = contaService.criar(request);

        assertNotNull(response);
        assertTrue(response.ativa());
        assertEquals("Banco Inter S.A.", response.instituicao());
        assertEquals(new BigDecimal("300.00"), response.saldoInicial());
        assertEquals(new BigDecimal("450.00"), response.saldoAtual());
    }

    @Test
    @DisplayName("Deve rejeitar criação quando já existir conta ativa com mesmo nome")
    void deveRejeitarQuandoContaAtivaJaExiste() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(contaRepository.findByUsuarioIdAndNomeIgnoreCase(usuarioPadrao.getId(), "Nubank"))
                .thenReturn(Optional.of(contaAtiva));

        ContaRequest request = new ContaRequest("Nubank", "Outro", TipoConta.CONTA_CORRENTE, BigDecimal.ZERO, true);

        assertThrows(RegraDeNegocioException.class, () -> contaService.criar(request));
        verify(contaRepository, never()).save(any(Conta.class));
    }

    @Test
    @DisplayName("Deve listar contas calculando o saldo atual dinâmico corretamente")
    void deveListarContasComSaldoAtualCalculado() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(contaRepository.findAllByUsuarioIdOrderByNomeAsc(usuarioPadrao.getId()))
                .thenReturn(List.of(contaAtiva));

        Object[] resultadoAgrupado = new Object[]{contaAtiva.getId(), new BigDecimal("250.50")};
        when(transacaoRepository.calcularSaldoTransacoesPorContaDoUsuario(usuarioPadrao.getId()))
                .thenReturn(List.<Object[]>of(resultadoAgrupado));

        List<ContaResponse> lista = contaService.listarTodas(null);

        assertEquals(1, lista.size());
        assertEquals(new BigDecimal("1000.00"), lista.getFirst().saldoInicial());
        assertEquals(new BigDecimal("1250.50"), lista.getFirst().saldoAtual());
    }
}
