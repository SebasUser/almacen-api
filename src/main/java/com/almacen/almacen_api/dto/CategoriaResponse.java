package com.almacen.almacen_api.dto;

import java.util.List;

public record CategoriaResponse(Long id, String nombre, List<String> atributos) {

}