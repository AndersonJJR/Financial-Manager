package com.anderson.financialmanager.model.conta;

import com.anderson.financialmanager.model.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contas", indexes = {
        @Index(name = "idx_contas_usuario_id", columnList = "usuario_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 100)
    private String instituicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoConta tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_contas_usuario"))
    private Usuario usuario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime atualizadoEm;
}
