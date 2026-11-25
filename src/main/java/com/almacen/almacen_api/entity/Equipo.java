package com.almacen.almacen_api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "equipos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Equipo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "codigo_tk", nullable = false, unique = true, length = 50)
    private String codigoTk;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    @Column(length = 80)
    private String marca;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoEquipo estado = EstadoEquipo.DISPONIBLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CondicionEquipo condicion = CondicionEquipo.POR_REVISAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "ubicacion_tipo", nullable = false)
    @Builder.Default
    private UbicacionTipo ubicacionTipo = UbicacionTipo.ALMACEN_X;

    @Column(name = "ubicacion_detalle", length = 150)
    private String ubicacionDetalle;

    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    @Column(length = 150)
    private String proveedor;

    @Column(length = 50)
    private String factura;

    @Column(length = 50)
    private String guia;

    @Column(name = "costo_usd", precision = 12, scale = 2)
    private BigDecimal costoUsd;

    @Column(name = "costo_pen", precision = 12, scale = 2)
    private BigDecimal costoPen;

    @Column(name = "tipo_cambio", precision = 8, scale = 4)
    private BigDecimal tipoCambio;

    @Column(length = 100)
    private String garantia;

    private String observaciones;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "equipo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EquipoAtributo> atributos = new ArrayList<>();
}