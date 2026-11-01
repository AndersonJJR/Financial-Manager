package com.anderson.financialmanager.model.conta;

import com.anderson.financialmanager.config.SecurityContextHelper;
import com.anderson.financialmanager.exception.RecursoNaoEncontradoException;
import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.conta.dto.ContaRequest;
import com.anderson.financialmanager.model.conta.dto.ContaResponse;
import com.anderson.financialmanager.model.transacao.TransacaoRepository;
import com.anderson.financialmanager.model.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional
    public ContaResponse criar(ContaRequest request) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        String nomeNormalizado = request.nome().trim();

        Optional<Conta> existenteOpt = contaRepository.findByUsuarioIdAndNomeIgnoreCase(usuario.getId(), nomeNormalizado);
        if (existenteOpt.isPresent()) {
            Conta existente = existenteOpt.get();
            if (Boolean.TRUE.equals(existente.getAtiva())) {
                throw new RegraDeNegocioException("Já existe uma conta ativa cadastrada com este nome para o usuário.");
            }
            // Reativação da conta inativa preservando o histórico existente
            existente.setInstituicao(request.instituicao() != null ? request.instituicao().trim() : null);
            existente.setTipo(request.tipo());
            if (request.saldoInicial() != null) {
                existente.setSaldoInicial(request.saldoInicial());
            }
            existente.setAtiva(true);
            Conta reativada = contaRepository.save(existente);
            return ContaResponse.fromEntity(reativada, calcularSaldoAtual(reativada, usuario.getId()));
        }

        Conta conta = Conta.builder()
                .nome(nomeNormalizado)
                .instituicao(request.instituicao() != null ? request.instituicao().trim() : null)
                .tipo(request.tipo())
                .saldoInicial(request.saldoInicial() != null ? request.saldoInicial() : BigDecimal.ZERO)
                .ativa(request.ativa() != null ? request.ativa() : true)
                .usuario(usuario)
                .build();

        Conta salva = contaRepository.save(conta);
        return ContaResponse.fromEntity(salva, salva.getSaldoInicial());
    }

    @Transactional(readOnly = true)
    public List<ContaResponse> listarTodas(Boolean ativa) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        List<Conta> contas = (ativa != null)
                ? contaRepository.findAllByUsuarioIdAndAtivaOrderByNomeAsc(usuario.getId(), ativa)
                : contaRepository.findAllByUsuarioIdOrderByNomeAsc(usuario.getId());

        List<Object[]> saldosAgrupados = transacaoRepository.calcularSaldoTransacoesPorContaDoUsuario(usuario.getId());
        Map<UUID, BigDecimal> mapaSaldos = new HashMap<>();
        for (Object[] linha : saldosAgrupados) {
            UUID contaId = (UUID) linha[0];
            BigDecimal delta = (BigDecimal) linha[1];
            mapaSaldos.put(contaId, delta != null ? delta : BigDecimal.ZERO);
        }

        return contas.stream()
                .map(c -> {
                    BigDecimal delta = mapaSaldos.getOrDefault(c.getId(), BigDecimal.ZERO);
                    BigDecimal saldoAtual = c.getSaldoInicial().add(delta);
                    return ContaResponse.fromEntity(c, saldoAtual);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ContaResponse buscarPorId(UUID id) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Conta conta = buscarEntidadeDoUsuario(id, usuario.getId());
        BigDecimal saldoAtual = calcularSaldoAtual(conta, usuario.getId());
        return ContaResponse.fromEntity(conta, saldoAtual);
    }

    @Transactional
    public ContaResponse atualizar(UUID id, ContaRequest request) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Conta conta = buscarEntidadeDoUsuario(id, usuario.getId());
        String novoNome = request.nome().trim();

        if (!conta.getNome().equalsIgnoreCase(novoNome)) {
            Optional<Conta> existenteComNovoNome = contaRepository.findByUsuarioIdAndNomeIgnoreCase(usuario.getId(), novoNome);
            if (existenteComNovoNome.isPresent() && !existenteComNovoNome.get().getId().equals(id)) {
                if (Boolean.TRUE.equals(existenteComNovoNome.get().getAtiva())) {
                    throw new RegraDeNegocioException("Já existe uma conta ativa cadastrada com este nome para o usuário.");
                }
                throw new RegraDeNegocioException("Já existe uma conta inativa com este nome. Considere reativá-la.");
            }
        }

        conta.setNome(novoNome);
        conta.setInstituicao(request.instituicao() != null ? request.instituicao().trim() : null);
        conta.setTipo(request.tipo());
        if (request.saldoInicial() != null) {
            conta.setSaldoInicial(request.saldoInicial());
        }
        if (request.ativa() != null) {
            conta.setAtiva(request.ativa());
        }

        Conta atualizada = contaRepository.save(conta);
        BigDecimal saldoAtual = calcularSaldoAtual(atualizada, usuario.getId());
        return ContaResponse.fromEntity(atualizada, saldoAtual);
    }

    @Transactional
    public void excluir(UUID id) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Conta conta = buscarEntidadeDoUsuario(id, usuario.getId());
        conta.setAtiva(false);
        contaRepository.save(conta);
    }

    @Transactional(readOnly = true)
    public Conta buscarEntidadeDoUsuario(UUID id, UUID usuarioId) {
        return contaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada para o identificador informado."));
    }

    private BigDecimal calcularSaldoAtual(Conta conta, UUID usuarioId) {
        BigDecimal delta = transacaoRepository.calcularSaldoTransacoesEfetivadasDaConta(conta.getId(), usuarioId);
        return conta.getSaldoInicial().add(delta != null ? delta : BigDecimal.ZERO);
    }
}
