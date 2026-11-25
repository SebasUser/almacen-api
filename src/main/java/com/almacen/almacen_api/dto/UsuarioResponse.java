package com.almacen.almacen_api.dto;

public record UsuarioResponse(Long id, String nombre, String username, String rol, Boolean activo, String foto) {

}
