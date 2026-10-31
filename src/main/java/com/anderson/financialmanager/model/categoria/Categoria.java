package com.anderson.financialmanager.model.categoria;

import com.anderson.financialmanager.model.transacao.TipoTransacao;
import com.anderson.financialmanager.model.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "categorias", indexes = {
        @Index(name = "idx_categorias_usuario_id", columnList = "usuario_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 250)
    private String descricao;

    @Column(length = 20)
    private String corHex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransacao tipo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_categorias_usuario"))
    private Usuario usuario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime atualizadoEm;
}
