package com.anderson.financialmanager.model.categoria;

import com.anderson.financialmanager.config.SecurityContextHelper;
import com.anderson.financialmanager.exception.RecursoNaoEncontradoException;
import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.categoria.dto.CategoriaRequest;
import com.anderson.financialmanager.model.categoria.dto.CategoriaResponse;
import com.anderson.financialmanager.model.transacao.TipoTransacao;
import com.anderson.financialmanager.model.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        String nomeNormalizado = request.nome().trim();

        Optional<Categoria> existenteOpt = categoriaRepository.findByUsuarioIdAndNomeIgnoreCaseAndTipo(
                usuario.getId(),
                nomeNormalizado,
                request.tipo()
        );

        if (existenteOpt.isPresent()) {
            Categoria existente = existenteOpt.get();
            if (Boolean.TRUE.equals(existente.getAtiva())) {
                throw new RegraDeNegocioException("Já existe uma categoria ativa cadastrada com este nome e tipo para o usuário.");
            }
            // Reativação da categoria inativa preservando o histórico existente
            existente.setDescricao(request.descricao() != null ? request.descricao().trim() : null);
            existente.setCorHex(request.corHex());
            existente.setAtiva(true);
            return CategoriaResponse.fromEntity(categoriaRepository.save(existente));
        }

        Categoria categoria = Categoria.builder()
                .nome(nomeNormalizado)
                .descricao(request.descricao() != null ? request.descricao().trim() : null)
                .corHex(request.corHex())
                .tipo(request.tipo())
                .ativa(request.ativa() != null ? request.ativa() : true)
                .usuario(usuario)
                .build();

        return CategoriaResponse.fromEntity(categoriaRepository.save(categoria));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodas(TipoTransacao tipo, Boolean ativa) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        List<Categoria> categorias;

        if (ativa != null) {
            categorias = (tipo != null)
                    ? categoriaRepository.findAllByUsuarioIdAndTipoAndAtivaOrderByNomeAsc(usuario.getId(), tipo, ativa)
                    : categoriaRepository.findAllByUsuarioIdAndAtivaOrderByNomeAsc(usuario.getId(), ativa);
        } else {
            categorias = (tipo != null)
                    ? categoriaRepository.findAllByUsuarioIdAndTipoOrderByNomeAsc(usuario.getId(), tipo)
                    : categoriaRepository.findAllByUsuarioIdOrderByNomeAsc(usuario.getId());
        }

        return categorias.stream()
                .map(CategoriaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(UUID id) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Categoria categoria = buscarEntidadeDoUsuario(id, usuario.getId());
        return CategoriaResponse.fromEntity(categoria);
    }

    @Transactional
    public CategoriaResponse atualizar(UUID id, CategoriaRequest request) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Categoria categoria = buscarEntidadeDoUsuario(id, usuario.getId());
        String novoNome = request.nome().trim();

        if (!categoria.getNome().equalsIgnoreCase(novoNome) || categoria.getTipo() != request.tipo()) {
            Optional<Categoria> existente = categoriaRepository.findByUsuarioIdAndNomeIgnoreCaseAndTipo(
                    usuario.getId(),
                    novoNome,
                    request.tipo()
            );
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                if (Boolean.TRUE.equals(existente.get().getAtiva())) {
                    throw new RegraDeNegocioException("Já existe uma categoria ativa cadastrada com este nome e tipo para o usuário.");
                }
                throw new RegraDeNegocioException("Já existe uma categoria inativa com este nome e tipo. Considere reativá-la.");
            }
        }

        categoria.setNome(novoNome);
        categoria.setDescricao(request.descricao() != null ? request.descricao().trim() : null);
        categoria.setCorHex(request.corHex());
        categoria.setTipo(request.tipo());
        if (request.ativa() != null) {
            categoria.setAtiva(request.ativa());
        }

        return CategoriaResponse.fromEntity(categoriaRepository.save(categoria));
    }

    @Transactional
    public void excluir(UUID id) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        Categoria categoria = buscarEntidadeDoUsuario(id, usuario.getId());
        categoria.setAtiva(false);
        categoriaRepository.save(categoria);
    }

    @Transactional(readOnly = true)
    public Categoria buscarEntidadeDoUsuario(UUID id, UUID usuarioId) {
        return categoriaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada para o identificador informado."));
    }
}
