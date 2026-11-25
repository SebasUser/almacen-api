package com.almacen.almacen_api.service;

import com.almacen.almacen_api.entity.Equipo;
import com.almacen.almacen_api.entity.Movimiento;
import com.almacen.almacen_api.entity.TipoMovimiento;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class MovimientoSpecs {

    private MovimientoSpecs() {}

    /**
     * Filtros de la lista global de movimientos. La búsqueda libre cubre lo que el almacén usa para ubicar
     * un movimiento: código y serie del equipo, cliente/persona/origen, OC, RQ, guía y factura.
     */
    public static Specification<Movimiento> filtrar(String q, TipoMovimiento tipo, Long usuarioId,
                                                    Boolean anulado, LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Join<Movimiento, Equipo> eq = root.join("equipo");
                p.add(cb.or(
                        cb.like(cb.lower(eq.get("codigoTk")), like),
                        cb.like(cb.lower(eq.get("numeroSerie")), like),
                        cb.like(cb.lower(root.get("origenDestino")), like),
                        cb.like(cb.lower(root.get("oc")), like),
                        cb.like(cb.lower(root.get("rq")), like),
                        cb.like(cb.lower(root.get("guia")), like),
                        cb.like(cb.lower(root.get("factura")), like)));
            }
            if (tipo != null) {
                p.add(cb.equal(root.get("tipo"), tipo));
            }
            if (usuarioId != null) {
                p.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }
            if (anulado != null) {
                p.add(cb.equal(root.get("anulado"), anulado));
            }
            if (desde != null) {
                p.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fecha"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                p.add(cb.lessThan(root.<LocalDateTime>get("fecha"), hasta.plusDays(1).atStartOfDay()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
