package com.almacen.almacen_api.controller;

import com.almacen.almacen_api.dto.CategoriaResponse;
import com.almacen.almacen_api.entity.CategoriaAtributo;
import com.almacen.almacen_api.repository.CategoriaAtributoRepository;
import com.almacen.almacen_api.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaRepository categoriaRepo;
    private final CategoriaAtributoRepository atributoRepo;

    @GetMapping
    public List<CategoriaResponse> listar() {
        return categoriaRepo.findByActivoTrueOrderByNombre().stream()
                .map(c -> new CategoriaResponse(
                        c.getId(),
                        c.getNombre(),
                        atributoRepo.findByCategoriaIdOrderByNombre(c.getId()).stream()
                                .map(CategoriaAtributo::getNombre).toList()))
                .toList();
    }
}