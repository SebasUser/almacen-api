package com.almacen.almacen_api.repository;

import com.almacen.almacen_api.entity.CategoriaAtributo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaAtributoRepository extends JpaRepository<CategoriaAtributo, Long> {
    List<CategoriaAtributo> findByCategoriaIdOrderByNombre(Long categoriaId);
    Optional<CategoriaAtributo> findByCategoriaIdAndNombreIgnoreCase(Long categoriaId, String nombre);
}