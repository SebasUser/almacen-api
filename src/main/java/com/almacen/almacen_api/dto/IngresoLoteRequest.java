package com.almacen.almacen_api.dto;

import com.almacen.almacen_api.entity.CondicionEquipo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** Ingreso de varios equipos al almacén X (por ejemplo, un traspaso desde el almacén M). */
public record IngresoLoteRequest(
        @NotNull(message = "Selecciona la categoría") Long categoriaId,
        @Size(max = 150) String origen,
        @Size(max = 50) String guia,
        @Size(max = 255) String observaciones,
        @NotEmpty(message = "Agrega al menos un equipo")
        @Size(max = 200, message = "Máximo 200 equipos por ingreso")
        @Valid
        List<ItemIngreso> items
) {
    public record ItemIngreso(
            @NotBlank @Size(max = 50) String codigoTk,
            @Size(max = 100) String numeroSerie,
            @Size(max = 80) String marca,
            @Size(max = 255) String descripcion,
            CondicionEquipo condicion,
            Map<String, String> atributos
    ) {}
}
