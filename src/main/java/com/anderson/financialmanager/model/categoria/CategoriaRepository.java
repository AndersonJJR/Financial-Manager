package com.anderson.financialmanager.model.categoria;

import com.anderson.financialmanager.model.transacao.TipoTransacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findAllByUsuarioIdOrderByNomeAsc(UUID usuarioId);

    List<Categoria> findAllByUsuarioIdAndAtivaOrderByNomeAsc(UUID usuarioId, Boolean ativa);

    List<Categoria> findAllByUsuarioIdAndTipoOrderByNomeAsc(UUID usuarioId, TipoTransacao tipo);

    List<Categoria> findAllByUsuarioIdAndTipoAndAtivaOrderByNomeAsc(UUID usuarioId, TipoTransacao tipo, Boolean ativa);

    Optional<Categoria> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    Optional<Categoria> findByUsuarioIdAndNomeIgnoreCaseAndTipo(UUID usuarioId, String nome, TipoTransacao tipo);

    boolean existsByUsuarioIdAndNomeIgnoreCaseAndTipo(UUID usuarioId, String nome, TipoTransacao tipo);
}
