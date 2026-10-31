package com.anderson.financialmanager.model.transacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    @Query("""
            SELECT t FROM Transacao t
            JOIN FETCH t.conta
            JOIN FETCH t.categoria
            WHERE t.id = :id AND t.usuario.id = :usuarioId
            """)
    Optional<Transacao> findByIdAndUsuarioIdWithDetails(@Param("id") UUID id, @Param("usuarioId") UUID usuarioId);

    @Query("""
            SELECT t FROM Transacao t
            JOIN FETCH t.conta c
            JOIN FETCH t.categoria cat
            WHERE t.usuario.id = :usuarioId
              AND (:dataInicio IS NULL OR t.dataCompetencia >= :dataInicio)
              AND (:dataFim IS NULL OR t.dataCompetencia <= :dataFim)
              AND (:tipo IS NULL OR t.tipo = :tipo)
              AND (:status IS NULL OR t.status = :status)
              AND (:contaId IS NULL OR c.id = :contaId)
              AND (:categoriaId IS NULL OR cat.id = :categoriaId)
            ORDER BY t.dataCompetencia DESC, t.criadoEm DESC
            """)
    List<Transacao> buscarComFiltros(
            @Param("usuarioId") UUID usuarioId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("tipo") TipoTransacao tipo,
            @Param("status") StatusTransacao status,
            @Param("contaId") UUID contaId,
            @Param("categoriaId") UUID categoriaId
    );

    @Query("""
            SELECT t FROM Transacao t
            JOIN FETCH t.conta c
            JOIN FETCH t.categoria cat
            WHERE t.usuario.id = :usuarioId
              AND t.dataCompetencia BETWEEN :dataInicio AND :dataFim
            ORDER BY t.dataCompetencia ASC, t.criadoEm ASC
            """)
    List<Transacao> findAllByUsuarioIdAndPeriodo(
            @Param("usuarioId") UUID usuarioId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );

    @Query("""
            SELECT t FROM Transacao t
            JOIN FETCH t.conta c
            JOIN FETCH t.categoria cat
            WHERE t.usuario.id = :usuarioId
              AND t.dataCompetencia BETWEEN :dataInicio AND :dataFim
              AND c.id IN :contasIds
            ORDER BY t.dataCompetencia ASC, t.criadoEm ASC
            """)
    List<Transacao> findAllByUsuarioIdAndPeriodoAndContas(
            @Param("usuarioId") UUID usuarioId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("contasIds") List<UUID> contasIds
    );

    @Query("""
            SELECT t FROM Transacao t
            JOIN FETCH t.conta
            JOIN FETCH t.categoria
            WHERE t.usuario.id = :usuarioId
              AND t.grupoParcelamentoId = :grupoId
            ORDER BY t.parcelaAtual ASC, t.dataCompetencia ASC
            """)
    List<Transacao> findAllByUsuarioIdAndGrupoParcelamentoId(
            @Param("usuarioId") UUID usuarioId,
            @Param("grupoId") UUID grupoId
    );
}
