package com.anderson.financialmanager.model.conta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContaRepository extends JpaRepository<Conta, UUID> {

    List<Conta> findAllByUsuarioIdOrderByNomeAsc(UUID usuarioId);

    List<Conta> findAllByUsuarioIdAndAtivaOrderByNomeAsc(UUID usuarioId, Boolean ativa);

    Optional<Conta> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    Optional<Conta> findByUsuarioIdAndNomeIgnoreCase(UUID usuarioId, String nome);

    boolean existsByUsuarioIdAndNomeIgnoreCase(UUID usuarioId, String nome);
}
