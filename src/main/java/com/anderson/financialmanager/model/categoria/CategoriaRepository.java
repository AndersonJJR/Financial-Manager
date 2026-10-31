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

    List<Categoria> findAllByUsuarioIdAndTipoOrderByNomeAsc(UUID usuarioId, TipoTransacao tipo);

    Optional<Categoria> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    boolean existsByUsuarioIdAndNomeIgnoreCaseAndTipo(UUID usuarioId, String nome, TipoTransacao tipo);
}
