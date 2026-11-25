package com.almacen.almacen_api.dto;

import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        Long equipoId,
        String codigoTk,
        String tipo,
        LocalDateTime fecha,
        String origenDestino,
        String oc,
        String rq,
        String guia,
        String factura,
        String observaciones,
        String usuario,
        Boolean anulado,
        String anuladoPor,
        LocalDateTime fechaAnulacion,
        String motivoAnulacion
) {}