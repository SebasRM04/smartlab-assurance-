package com.utp.smartlab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEquipo estado;

    // Detecta operaciones concurrentes sin bloquear el registro REQUIRES_NEW de conflictos.
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long version = 0L;

    // Indicador: recomendación de mantenimiento por horas de uso
    @Column(name = "contador_uso_horas", nullable = false)
    @Builder.Default
    private Integer contadorUsoHoras = 0;
}
