package com.almacen.almacen_api.repository;

import com.almacen.almacen_api.entity.EquipoEliminado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipoEliminadoRepository extends JpaRepository<EquipoEliminado, Long> {

    Page<EquipoEliminado> findByCodigoTkContainingIgnoreCase(String codigoTk, Pageable pageable);
}
