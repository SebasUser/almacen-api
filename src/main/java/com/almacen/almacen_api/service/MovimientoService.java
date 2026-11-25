package com.almacen.almacen_api.service;

import com.almacen.almacen_api.dto.MovimientoLoteRequest;
import com.almacen.almacen_api.dto.MovimientoRequest;
import com.almacen.almacen_api.dto.MovimientoResponse;
import com.almacen.almacen_api.dto.PaginaResponse;
import com.almacen.almacen_api.dto.UsuarioResumen;
import com.almacen.almacen_api.entity.*;
import com.almacen.almacen_api.exception.ApiException;
import com.almacen.almacen_api.repository.EquipoRepository;
import com.almacen.almacen_api.repository.MovimientoRepository;
import com.almacen.almacen_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final EquipoRepository equipoRepo;
    private final MovimientoRepository movimientoRepo;
    private final UsuarioRepository usuarioRepo;

    @Transactional
    public MovimientoResponse registrar(Long equipoId, MovimientoRequest req, String username) {
        Equipo e = equipoRepo.buscarParaActualizar(equipoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el equipo con id " + equipoId));

        if (e.getEstado() == EstadoEquipo.VENDIDO || e.getEstado() == EstadoEquipo.BAJA) {
            throw ApiException.conflicto("El equipo está " + e.getEstado()
                    + " y ya no admite movimientos");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fecha = req.fecha() != null ? req.fecha() : ahora;
        if (fecha.isAfter(ahora.plusMinutes(5))) {
            throw ApiException.badRequest("La fecha del movimiento no puede ser futura");
        }

        String destino = limpiar(req.origenDestino());
        TipoMovimiento tipo = req.tipo();

        switch (tipo) {
            case ENTRADA -> throw ApiException.badRequest(
                    "La entrada se registra automáticamente al crear el equipo");
            case SALIDA_ALQUILER -> {
                exigirEstado(e, tipo, EstadoEquipo.DISPONIBLE);
                exigirOperativo(e, "alquilar");
                exigirDestino(destino, "el cliente");
                aplicar(e, EstadoEquipo.ALQUILADO, UbicacionTipo.CLIENTE, destino);
            }
            case SALIDA_VENTA -> {
                exigirEstado(e, tipo, EstadoEquipo.DISPONIBLE, EstadoEquipo.ALQUILADO);
                if (e.getCondicion() == CondicionEquipo.NO_OPERATIVO) {
                    throw ApiException.conflicto("El equipo está NO OPERATIVO y no se puede vender así. "
                            + "Si se vende en ese estado, un administrador debe cambiar antes su condición a SOLO VENTA");
                }
                exigirDestino(destino, "el cliente");
                aplicar(e, EstadoEquipo.VENDIDO, UbicacionTipo.CLIENTE, destino);
            }
            case ASIGNACION_PERSONAL -> {
                exigirEstado(e, tipo, EstadoEquipo.DISPONIBLE);
                exigirOperativo(e, "asignar");
                exigirDestino(destino, "la persona");
                aplicar(e, EstadoEquipo.ASIGNADO, UbicacionTipo.PERSONAL, destino);
            }
            case DEVOLUCION_CLIENTE -> {
                exigirEstado(e, tipo, EstadoEquipo.ALQUILADO);
                aplicar(e, EstadoEquipo.DISPONIBLE, UbicacionTipo.ALMACEN_X, null);
            }
            case RETORNO_PERSONAL -> {
                exigirEstado(e, tipo, EstadoEquipo.ASIGNADO);
                aplicar(e, EstadoEquipo.DISPONIBLE, UbicacionTipo.ALMACEN_X, null);
            }
        }

        Movimiento m = Movimiento.builder()
                .equipo(e)
                .tipo(tipo)
                .fecha(fecha)
                .origenDestino(destino != null ? destino : origenPorDefecto(tipo))
                .oc(limpiar(req.oc()))
                .rq(limpiar(req.rq()))
                .guia(limpiar(req.guia()))
                .factura(limpiar(req.factura()))
                .observaciones(limpiar(req.observaciones()))
                .usuario(usuarioRepo.findByUsername(username).orElse(null))
                .build();
        return aResponse(movimientoRepo.save(m));
    }

    /**
     * Registra el mismo movimiento para varios equipos en una sola operación: o se registran todos o no se
     * registra ninguno (si uno no admite el movimiento, se avisa cuál y no queda nada a medias).
     */
    @Transactional
    public List<MovimientoResponse> registrarLote(MovimientoLoteRequest req, String username) {
        List<Long> ids = req.equipoIds().stream().distinct().toList();
        MovimientoRequest base = new MovimientoRequest(req.tipo(), req.origenDestino(), req.fecha(),
                req.oc(), req.rq(), req.guia(), req.factura(), req.observaciones());

        List<MovimientoResponse> resultado = new ArrayList<>();
        for (Long id : ids) {
            try {
                resultado.add(registrar(id, base, username));
            } catch (ApiException e) {
                String codigo = equipoRepo.findById(id).map(Equipo::getCodigoTk).orElse("#" + id);
                throw new ApiException(e.getStatus(), "Equipo " + codigo + ": " + e.getMessage()
                        + ". No se registró ningún movimiento del lote");
            }
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> historial(Long equipoId) {
        if (!equipoRepo.existsById(equipoId)) {
            throw ApiException.noEncontrado("No existe el equipo con id " + equipoId);
        }
        return movimientoRepo.findByEquipoIdOrderByFechaDescIdDesc(equipoId)
                .stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoResponse> listar(String q, TipoMovimiento tipo, Long usuarioId,
                                                     Boolean anulado, LocalDate desde, LocalDate hasta,
                                                     Pageable pageable) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw ApiException.badRequest("La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }
        Page<Movimiento> page = movimientoRepo.findAll(
                MovimientoSpecs.filtrar(q, tipo, usuarioId, anulado, desde, hasta), pageable);
        return new PaginaResponse<>(
                page.getContent().stream().map(this::aResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /** Usuarios que han registrado al menos un movimiento (para el filtro "registrado por"). */
    @Transactional(readOnly = true)
    public List<UsuarioResumen> usuariosConMovimientos() {
        return movimientoRepo.listarUsuariosConMovimientos();
    }

    /**
     * Anula un movimiento equivocado (solo ADMIN, desde el controlador). El movimiento se conserva en el
     * historial marcado como anulado y el equipo vuelve al estado que dejó el movimiento vigente anterior.
     * Solo se puede anular el último movimiento vigente, para que el estado del equipo siga siendo coherente.
     */
    @Transactional
    public MovimientoResponse anular(Long movimientoId, String motivo, String username) {
        String razon = limpiar(motivo);
        if (razon == null || razon.length() < 5) {
            throw ApiException.badRequest("Indica el motivo de la anulación (mínimo 5 caracteres)");
        }
        if (razon.length() > 255) {
            throw ApiException.badRequest("El motivo es demasiado largo (máximo 255 caracteres)");
        }

        Movimiento m = movimientoRepo.findById(movimientoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el movimiento con id " + movimientoId));
        Long equipoId = m.getEquipo().getId();
        Equipo e = equipoRepo.buscarParaActualizar(equipoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el equipo con id " + equipoId));

        if (Boolean.TRUE.equals(m.getAnulado())) {
            throw ApiException.conflicto("El movimiento ya está anulado");
        }
        if (m.getTipo() == TipoMovimiento.ENTRADA) {
            throw ApiException.badRequest(
                    "La entrada de alta no se puede anular; si el equipo se registró por error, elimínalo");
        }
        if (e.getEstado() == EstadoEquipo.BAJA) {
            throw ApiException.conflicto("El equipo está dado de baja y no admite cambios");
        }

        List<Movimiento> vigentes = movimientoRepo.findByEquipoIdAndAnuladoFalseOrderByIdDesc(equipoId);
        if (vigentes.isEmpty() || !vigentes.get(0).getId().equals(m.getId())) {
            throw ApiException.conflicto(
                    "Solo se puede anular el último movimiento vigente del equipo. Anula primero los más recientes");
        }
        Movimiento anterior = vigentes.size() > 1 ? vigentes.get(1) : null;

        String quien = usuarioRepo.findByUsername(username).map(Usuario::getNombre).orElse(username);
        m.setAnulado(true);
        m.setAnuladoPor(quien);
        m.setFechaAnulacion(LocalDateTime.now());
        m.setMotivoAnulacion(razon);

        restaurarEstado(e, anterior);
        return aResponse(m);
    }

    // ---------- utilidades ----------

    /** Deja el equipo como lo dejó el último movimiento vigente (o disponible en el almacén si no hay otro). */
    private void restaurarEstado(Equipo e, Movimiento ultimoVigente) {
        if (ultimoVigente == null) {
            aplicar(e, EstadoEquipo.DISPONIBLE, UbicacionTipo.ALMACEN_X, null);
            return;
        }
        String destino = limpiar(ultimoVigente.getOrigenDestino());
        switch (ultimoVigente.getTipo()) {
            case ENTRADA, DEVOLUCION_CLIENTE, RETORNO_PERSONAL ->
                    aplicar(e, EstadoEquipo.DISPONIBLE, UbicacionTipo.ALMACEN_X, null);
            case SALIDA_ALQUILER -> aplicar(e, EstadoEquipo.ALQUILADO, UbicacionTipo.CLIENTE, destino);
            case SALIDA_VENTA -> aplicar(e, EstadoEquipo.VENDIDO, UbicacionTipo.CLIENTE, destino);
            case ASIGNACION_PERSONAL -> aplicar(e, EstadoEquipo.ASIGNADO, UbicacionTipo.PERSONAL, destino);
        }
    }

    private void exigirEstado(Equipo e, TipoMovimiento tipo, EstadoEquipo... permitidos) {
        if (!Arrays.asList(permitidos).contains(e.getEstado())) {
            throw ApiException.conflicto("No se puede registrar " + tipo + " con el equipo en estado "
                    + e.getEstado() + " (estados permitidos: " + Arrays.toString(permitidos) + ")");
        }
    }

    /** Solo los equipos OPERATIVOS salen en alquiler o se asignan a personal. */
    private void exigirOperativo(Equipo e, String accion) {
        if (e.getCondicion() != CondicionEquipo.OPERATIVO) {
            String motivo = switch (e.getCondicion()) {
                case SOLO_VENTA -> "es SOLO VENTA";
                case NO_OPERATIVO -> "está NO OPERATIVO";
                default -> "aún está POR REVISAR";
            };
            throw ApiException.conflicto("No se puede " + accion + " el equipo porque " + motivo
                    + ". Solo se " + accion + "n equipos OPERATIVOS; un administrador debe actualizar su condición");
        }
    }

    private void exigirDestino(String destino, String quien) {
        if (destino == null) {
            throw ApiException.badRequest("Debes indicar " + quien + " en 'origenDestino'");
        }
    }

    private void aplicar(Equipo e, EstadoEquipo estado, UbicacionTipo ubicacion, String detalle) {
        e.setEstado(estado);
        e.setUbicacionTipo(ubicacion);
        e.setUbicacionDetalle(detalle);
    }

    private String origenPorDefecto(TipoMovimiento tipo) {
        return switch (tipo) {
            case DEVOLUCION_CLIENTE, RETORNO_PERSONAL -> "Almacén X";
            default -> null;
        };
    }

    private String limpiar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private MovimientoResponse aResponse(Movimiento m) {
        return new MovimientoResponse(
                m.getId(), m.getEquipo().getId(), m.getEquipo().getCodigoTk(),
                m.getTipo().name(), m.getFecha(), m.getOrigenDestino(),
                m.getOc(), m.getRq(), m.getGuia(), m.getFactura(), m.getObservaciones(),
                m.getUsuario() != null ? m.getUsuario().getNombre() : null,
                m.getAnulado(), m.getAnuladoPor(), m.getFechaAnulacion(), m.getMotivoAnulacion());
    }
}