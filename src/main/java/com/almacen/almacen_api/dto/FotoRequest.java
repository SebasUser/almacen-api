package com.almacen.almacen_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FotoRequest(
        @NotBlank(message = "Selecciona una imagen")
        @Size(max = 15000, message = "La imagen es demasiado grande")
        String foto
) {}
