package com.anderson.financialmanager.model.transacao;

import com.anderson.financialmanager.model.categoria.Categoria;
import com.anderson.financialmanager.model.conta.Conta;
import com.anderson.financialmanager.model.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transacoes", indexes = {
        @Index(name = "idx_transacoes_usuario_competencia", columnList = "usuario_id, data_competencia"),
        @Index(name = "idx_transacoes_conta_id", columnList = "conta_id"),
        @Index(name = "idx_transacoes_categoria_id", columnList = "categoria_id"),
        @Index(name = "idx_transacoes_grupo_parcelamento", columnList = "grupo_parcelamento_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_competencia", nullable = false)
    private LocalDate dataCompetencia;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusTransacao status = StatusTransacao.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ModalidadeTransacao modalidade = ModalidadeTransacao.UNICA;

    @Column(name = "grupo_parcelamento_id")
    private UUID grupoParcelamentoId;

    @Column(name = "parcela_atual")
    private Integer parcelaAtual;

    @Column(name = "total_parcelas")
    private Integer totalParcelas;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Periodicidade periodicidade;

    @Column(name = "recorrencia_ativa", nullable = false)
    @Builder.Default
    private Boolean recorrenciaAtiva = false;

    @Column(length = 500)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transacoes_conta"))
    private Conta conta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transacoes_categoria"))
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transacoes_usuario"))
    private Usuario usuario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    public String getReferenciaParcela() {
        if (parcelaAtual != null && totalParcelas != null) {
            return parcelaAtual + "/" + totalParcelas;
        }
        return null;
    }
}
