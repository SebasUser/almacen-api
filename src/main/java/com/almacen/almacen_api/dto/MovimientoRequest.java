package com.almacen.almacen_api.dto;

import com.almacen.almacen_api.entity.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record MovimientoRequest(
        @NotNull TipoMovimiento tipo,
        @Size(max = 150) String origenDestino,
        LocalDateTime fecha,
        @Size(max = 50) String oc,
        @Size(max = 50) String rq,
        @Size(max = 50) String guia,
        @Size(max = 50) String factura,
        @Size(max = 255) String observaciones
) {}