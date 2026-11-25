package com.almacen.almacen_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularRequest(
        @NotBlank(message = "Indica el motivo de la anulación")
        @Size(min = 5, max = 255, message = "El motivo debe tener entre 5 y 255 caracteres")
        String motivo
) {}
