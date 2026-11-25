package com.almacen.almacen_api.service;

import com.almacen.almacen_api.entity.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class EquipoSpecs {

    private EquipoSpecs() {}

    public static Specification<Equipo> filtrar(String q, Long categoriaId, String marca,
                                                String pulgadas, EstadoEquipo estado,
                                                CondicionEquipo condicion,
                                                UbicacionTipo ubicacionTipo,
                                                LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("codigoTk")), like),
                        cb.like(cb.lower(root.get("numeroSerie")), like),
                        cb.like(cb.lower(root.get("descripcion")), like)));
            }
            if (categoriaId != null) {
                p.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            }
            if (marca != null && !marca.isBlank()) {
                p.add(cb.equal(cb.upper(root.get("marca")), marca.trim().toUpperCase()));
            }
            if (estado != null) {
                p.add(cb.equal(root.get("estado"), estado));
            }
            if (condicion != null) {
                p.add(cb.equal(root.get("condicion"), condicion));
            }
            if (ubicacionTipo != null) {
                p.add(cb.equal(root.get("ubicacionTipo"), ubicacionTipo));
            }
            // Fecha de registro del equipo (día completo, hora local), ambos extremos incluidos
            if (desde != null) {
                p.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                p.add(cb.lessThan(root.<LocalDateTime>get("createdAt"), hasta.plusDays(1).atStartOfDay()));
            }
            if (pulgadas != null && !pulgadas.isBlank()) {
                Subquery<Long> sq = query.subquery(Long.class);
                Root<EquipoAtributo> a = sq.from(EquipoAtributo.class);
                sq.select(cb.literal(1L)).where(
                        cb.equal(a.get("equipo"), root),
                        cb.equal(cb.lower(a.get("atributo").get("nombre")), "pulgadas"),
                        cb.equal(a.get("valor"), pulgadas.trim()));
                p.add(cb.exists(sq));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}