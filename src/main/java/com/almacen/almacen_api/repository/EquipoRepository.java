package com.almacen.almacen_api.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import com.almacen.almacen_api.entity.Equipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Long>, JpaSpecificationExecutor<Equipo> {

    boolean existsByCodigoTkIgnoreCase(String codigoTk);

    Optional<Equipo> findByCodigoTkIgnoreCase(String codigoTk);

    List<Equipo> findByNumeroSerieIgnoreCase(String numeroSerie);

    boolean existsByCodigoTkIgnoreCaseAndIdNot(String codigoTk, Long id);

    @Query("select distinct e.marca from Equipo e where e.marca is not null order by e.marca")
    List<String> listarMarcas();

    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Equipo> findAll(Specification<Equipo> spec, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipo e where e.id = :id")
    Optional<Equipo> buscarParaActualizar(@Param("id") Long id);
}
