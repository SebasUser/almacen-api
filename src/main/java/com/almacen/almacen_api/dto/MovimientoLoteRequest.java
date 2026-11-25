package com.almacen.almacen_api.dto;

import com.almacen.almacen_api.entity.TipoMovimiento;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** El mismo movimiento (con los mismos documentos) aplicado a varios equipos a la vez. */
public record MovimientoLoteRequest(
        @NotNull TipoMovimiento tipo,
        @Size(max = 150) String origenDestino,
        LocalDateTime fecha,
        @Size(max = 50) String oc,
        @Size(max = 50) String rq,
        @Size(max = 50) String guia,
        @Size(max = 50) String factura,
        @Size(max = 255) String observaciones,
        @NotEmpty(message = "Agrega al menos un equipo")
        @Size(max = 200, message = "Máximo 200 equipos por lote")
        List<Long> equipoIds
) {}
