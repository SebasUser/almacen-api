package com.almacen.almacen_api.dto;

import java.time.LocalDateTime;

public record EquipoEliminadoResponse(
        Long id,
        Long equipoId,
        String codigoTk,
        String categoria,
        String marca,
        String numeroSerie,
        String descripcion,
        String estado,
        String ubicacion,
        Integer totalMovimientos,
        String detalle,
        String motivo,
        String eliminadoPor,
        LocalDateTime fechaEliminacion
) {}
