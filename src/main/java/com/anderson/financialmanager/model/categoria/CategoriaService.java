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

        if (categoriaRepository.existsByUsuarioIdAndNomeIgnoreCaseAndTipo(usuario.getId(), nomeNormalizado, request.tipo())) {
            throw new RegraDeNegocioException("Já existe uma categoria cadastrada com este nome e tipo para o usuário.");
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
    public List<CategoriaResponse> listarTodas(TipoTransacao tipo) {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        List<Categoria> categorias = (tipo != null)
                ? categoriaRepository.findAllByUsuarioIdAndTipoOrderByNomeAsc(usuario.getId(), tipo)
                : categoriaRepository.findAllByUsuarioIdOrderByNomeAsc(usuario.getId());

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

        if ((!categoria.getNome().equalsIgnoreCase(novoNome) || categoria.getTipo() != request.tipo())
                && categoriaRepository.existsByUsuarioIdAndNomeIgnoreCaseAndTipo(usuario.getId(), novoNome, request.tipo())) {
            throw new RegraDeNegocioException("Já existe uma categoria cadastrada com este nome e tipo para o usuário.");
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
