package com.almacen.almacen_api.dto;

import com.almacen.almacen_api.entity.CondicionEquipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record EquipoRequest(
        Long categoriaId,
        @NotBlank @Size(max = 50) String codigoTk,
        @Size(max = 100) String numeroSerie,
        @Size(max = 80) String marca,
        @Size(max = 255) String descripcion,
        LocalDate fechaCompra,
        @Size(max = 150) String proveedor,
        @Size(max = 50) String factura,
        @Size(max = 50) String guia,
        @PositiveOrZero BigDecimal costoUsd,
        @PositiveOrZero BigDecimal costoPen,
        @PositiveOrZero BigDecimal tipoCambio,
        @Size(max = 100) String garantia,
        @Size(max = 255) String observaciones,
        CondicionEquipo condicion,
        @Size(max = 150) String origenIngreso,
        Map<String, String> atributos
) {}