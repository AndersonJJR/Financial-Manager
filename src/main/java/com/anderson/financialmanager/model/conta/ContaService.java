package com.anderson.financialmanager.model.conta;

import com.anderson.financialmanager.config.SecurityContextHelper;
import com.anderson.financialmanager.exception.RecursoNaoEncontradoException;
import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.conta.dto.ContaRequest;
import com.anderson.financialmanager.model.conta.dto.ContaResponse;
import com.anderson.financialmanager.model.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional
    public ContaResponse criar(ContaRequest request) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        String nomeNormalizado = request.nome().trim();

        if (contaRepository.existsByUsuarioIdAndNomeIgnoreCase(usuario.getId(), nomeNormalizado)) {
            throw new RegraDeNegocioException("Já existe uma conta cadastrada com este nome para o usuário.");
        }

        Conta conta = Conta.builder()
                .nome(nomeNormalizado)
                .instituicao(request.instituicao() != null ? request.instituicao().trim() : null)
                .tipo(request.tipo())
                .saldoInicial(request.saldoInicial() != null ? request.saldoInicial() : BigDecimal.ZERO)
                .ativa(request.ativa() != null ? request.ativa() : true)
                .usuario(usuario)
                .build();

        return ContaResponse.fromEntity(contaRepository.save(conta));
    }

    @Transactional(readOnly = true)
    public List<ContaResponse> listarTodas() {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        return contaRepository.findAllByUsuarioIdOrderByNomeAsc(usuario.getId())
                .stream()
                .map(ContaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContaResponse buscarPorId(UUID id) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Conta conta = buscarEntidadeDoUsuario(id, usuario.getId());
        return ContaResponse.fromEntity(conta);
    }

    @Transactional
    public ContaResponse atualizar(UUID id, ContaRequest request) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Conta conta = buscarEntidadeDoUsuario(id, usuario.getId());
        String novoNome = request.nome().trim();

        if (!conta.getNome().equalsIgnoreCase(novoNome)
                && contaRepository.existsByUsuarioIdAndNomeIgnoreCase(usuario.getId(), novoNome)) {
            throw new RegraDeNegocioException("Já existe uma conta cadastrada com este nome para o usuário.");
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

        return ContaResponse.fromEntity(contaRepository.save(conta));
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
}
