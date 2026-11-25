package com.almacen.almacen_api.dto;

import com.almacen.almacen_api.entity.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioActualizarRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull Rol rol,
        @NotNull Boolean activo,
        /** Opcional: si viene, cambia el nombre de usuario (inicio de sesión). */
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
                message = "Usa de 3 a 50 caracteres: letras, números, punto, guion o guion bajo")
        String username
) {}