package com.almacen.almacen_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Constancia de un equipo eliminado: copia de sus datos y de su historial al momento de borrarlo. */
@Entity
@Table(name = "equipos_eliminados")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipoEliminado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipo_id", nullable = false)
    private Long equipoId;

    @Column(name = "codigo_tk", nullable = false, length = 50)
    private String codigoTk;

    @Column(nullable = false, length = 50)
    private String categoria;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    @Column(length = 80)
    private String marca;

    private String descripcion;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(length = 200)
    private String ubicacion;

    @Column(name = "total_movimientos", nullable = false)
    private Integer totalMovimientos;

    @Column(nullable = false, length = 8000)
    private String detalle;

    @Column(nullable = false)
    private String motivo;

    @Column(name = "eliminado_por", nullable = false, length = 100)
    private String eliminadoPor;

    @Column(name = "fecha_eliminacion", nullable = false)
    private LocalDateTime fechaEliminacion;
}
