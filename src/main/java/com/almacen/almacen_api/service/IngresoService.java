package com.almacen.almacen_api.service;

import com.almacen.almacen_api.dto.EquipoRequest;
import com.almacen.almacen_api.dto.IngresoLoteRequest;
import com.almacen.almacen_api.dto.IngresoLoteResponse;
import com.almacen.almacen_api.dto.MovimientoRequest;
import com.almacen.almacen_api.entity.Equipo;
import com.almacen.almacen_api.entity.TipoMovimiento;
import com.almacen.almacen_api.exception.ApiException;
import com.almacen.almacen_api.repository.EquipoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Ingreso de equipos al almacén X en lote. Para cada código:
 * - si no existe, se registra como equipo nuevo con su movimiento de entrada;
 * - si existe y estaba alquilado, se registra la devolución del cliente;
 * - si existe y estaba asignado a una persona, se registra el retorno de personal;
 * - si ya está en el almacén, vendido o dado de baja, no se puede ingresar.
 * Todo ocurre en una sola transacción: o entran todos o no entra ninguno.
 */
@Service
@RequiredArgsConstructor
public class IngresoService {

    private static final String ORIGEN_POR_DEFECTO = "Almacén M";

    private final EquipoService equipoService;
    private final MovimientoService movimientoService;
    private final EquipoRepository equipoRepo;

    @Transactional
    public IngresoLoteResponse ingresar(IngresoLoteRequest req, String username) {
        String origen = limpiar(req.origen());
        if (origen == null) {
            origen = ORIGEN_POR_DEFECTO;
        }
        String guia = limpiar(req.guia());
        String observaciones = limpiar(req.observaciones());

        Set<String> vistos = new HashSet<>();
        for (IngresoLoteRequest.ItemIngreso item : req.items()) {
            String codigo = item.codigoTk().trim().toUpperCase();
            if (!vistos.add(codigo)) {
                throw ApiException.badRequest("El código " + codigo + " está repetido en el lote");
            }
        }

        List<IngresoLoteResponse.Resultado> resultados = new ArrayList<>();
        for (IngresoLoteRequest.ItemIngreso item : req.items()) {
            String codigo = item.codigoTk().trim().toUpperCase();
            try {
                resultados.add(procesar(item, codigo, req.categoriaId(), origen, guia, observaciones, username));
            } catch (ApiException e) {
                throw new ApiException(e.getStatus(), "Equipo " + codigo + ": " + e.getMessage()
                        + ". No se registró ningún ingreso del lote");
            }
        }
        return new IngresoLoteResponse(resultados);
    }

    private IngresoLoteResponse.Resultado procesar(IngresoLoteRequest.ItemIngreso item, String codigo,
                                                   Long categoriaId, String origen, String guia,
                                                   String observaciones, String username) {
        Optional<Equipo> existente = equipoRepo.findByCodigoTkIgnoreCase(codigo);

        if (existente.isEmpty()) {
            EquipoRequest nuevo = new EquipoRequest(categoriaId, codigo, limpiar(item.numeroSerie()),
                    limpiar(item.marca()), limpiar(item.descripcion()),
                    null, null, null, null, null, null, null, null, null,
                    item.condicion(), null, item.atributos());
            Long id = equipoService.crearConEntrada(nuevo, username, origen, guia, observaciones);
            return new IngresoLoteResponse.Resultado(codigo, id, "NUEVO");
        }

        Equipo equipo = existente.get();
        TipoMovimiento tipo = switch (equipo.getEstado()) {
            case ALQUILADO -> TipoMovimiento.DEVOLUCION_CLIENTE;
            case ASIGNADO -> TipoMovimiento.RETORNO_PERSONAL;
            case DISPONIBLE -> throw ApiException.conflicto("ya está en el almacén X");
            case VENDIDO -> throw ApiException.conflicto("está vendido y no vuelve al almacén");
            case BAJA -> throw ApiException.conflicto("está dado de baja");
        };
        movimientoService.registrar(equipo.getId(),
                new MovimientoRequest(tipo, origen, null, null, null, guia, null, observaciones), username);
        return new IngresoLoteResponse.Resultado(codigo, equipo.getId(),
                tipo == TipoMovimiento.DEVOLUCION_CLIENTE ? "DEVOLUCION" : "RETORNO");
    }

    private String limpiar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
