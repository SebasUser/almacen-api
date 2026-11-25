package com.almacen.almacen_api.repository;

import com.almacen.almacen_api.entity.EquipoAtributo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EquipoAtributoRepository
        extends JpaRepository<EquipoAtributo, EquipoAtributo.EquipoAtributoId> {

    @Query("select distinct a.valor from EquipoAtributo a "
            + "where lower(a.atributo.nombre) = 'pulgadas' order by a.valor")
    List<String> listarPulgadas();
}