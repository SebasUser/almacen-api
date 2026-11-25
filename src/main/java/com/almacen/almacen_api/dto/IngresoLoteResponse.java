package com.almacen.almacen_api.dto;

import java.util.List;

public record IngresoLoteResponse(List<Resultado> resultados) {

    /** accion: NUEVO (equipo registrado), DEVOLUCION (volvió de un cliente) o RETORNO (volvió de una persona). */
    public record Resultado(String codigoTk, Long equipoId, String accion) {}
}
