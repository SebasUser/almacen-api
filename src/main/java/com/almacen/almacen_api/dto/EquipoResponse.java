package com.almacen.almacen_api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record EquipoResponse(
        Long id,
        Long categoriaId,
        String categoria,
        String codigoTk,
        String numeroSerie,
        String marca,
        String descripcion,
        String estado,
        String condicion,
        String ubicacionTipo,
        String ubicacionDetalle,
        LocalDate fechaCompra,
        String proveedor,
        String factura,
        String guia,
        BigDecimal costoUsd,
        BigDecimal costoPen,
        BigDecimal tipoCambio,
        String garantia,
        String observaciones,
        Map<String, String> atributos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}