package com.almacen.almacen_api.repository;

import com.almacen.almacen_api.dto.UsuarioResumen;
import com.almacen.almacen_api.entity.Movimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long>, JpaSpecificationExecutor<Movimiento> {

    void deleteByEquipoId(Long equipoId);

    /** Movimientos vigentes (no anulados) de un equipo, del más reciente al más antiguo según su registro. */
    List<Movimiento> findByEquipoIdAndAnuladoFalseOrderByIdDesc(Long equipoId);

    @EntityGraph(attributePaths = "usuario")
    List<Movimiento> findByEquipoIdOrderByFechaDescIdDesc(Long equipoId);

    @Override
    @EntityGraph(attributePaths = {"equipo", "usuario"})
    Page<Movimiento> findAll(Specification<Movimiento> spec, Pageable pageable);

    @Query("select distinct new com.almacen.almacen_api.dto.UsuarioResumen(u.id, u.nombre) "
            + "from Movimiento m join m.usuario u order by u.nombre")
    List<UsuarioResumen> listarUsuariosConMovimientos();
}
