package com.almacen.almacen_api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categoria_atributos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoriaAtributo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false, length = 50)
    private String nombre;
}
