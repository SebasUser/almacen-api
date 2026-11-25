package com.almacen.almacen_api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Movimiento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipo_id")
    private Equipo equipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "origen_destino", length = 150)
    private String origenDestino;

    @Column(length = 50)
    private String oc;

    @Column(length = 50)
    private String rq;

    @Column(length = 50)
    private String guia;

    @Column(length = 50)
    private String factura;

    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Un movimiento equivocado no se borra: se anula y queda visible en el historial. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean anulado = false;

    @Column(name = "anulado_por", length = 100)
    private String anuladoPor;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;
}
