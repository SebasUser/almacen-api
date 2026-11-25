package com.almacen.almacen_api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "equipo_atributos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipoAtributo {

    @EmbeddedId
    private EquipoAtributoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("equipoId")
    @JoinColumn(name = "equipo_id")
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("atributoId")
    @JoinColumn(name = "atributo_id")
    private CategoriaAtributo atributo;

    @Column(nullable = false, length = 100)
    private String valor;

    @Embeddable
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class EquipoAtributoId implements Serializable {
        private Long equipoId;
        private Long atributoId;
    }
}