package com.almacen.almacen_api.repository;

import com.almacen.almacen_api.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByActivoTrueOrderByNombre();
}