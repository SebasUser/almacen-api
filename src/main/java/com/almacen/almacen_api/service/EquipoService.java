package com.almacen.almacen_api.service;

import com.almacen.almacen_api.dto.EquipoRequest;
import com.almacen.almacen_api.dto.EquipoResponse;
import com.almacen.almacen_api.dto.PaginaResponse;
import com.almacen.almacen_api.entity.*;
import com.almacen.almacen_api.exception.ApiException;
import com.almacen.almacen_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EquipoService {

    private final EquipoRepository equipoRepo;
    private final CategoriaRepository categoriaRepo;
    private final CategoriaAtributoRepository atributoRepo;
    private final EquipoAtributoRepository equipoAtributoRepo;
    private final MovimientoRepository movimientoRepo;
    private final UsuarioRepository usuarioRepo;
    private final EquipoEliminadoRepository equipoEliminadoRepo;

    @Transactional(readOnly = true)
    public PaginaResponse<EquipoResponse> listar(String q, Long categoriaId, String marca,
                                                 String pulgadas, EstadoEquipo estado,
                                                 CondicionEquipo condicion,
                                                 UbicacionTipo ubicacionTipo,
                                                 LocalDate desde, LocalDate hasta, Pageable pageable) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw ApiException.badRequest("La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }
        Page<Equipo> page = equipoRepo.findAll(
                EquipoSpecs.filtrar(q, categoriaId, marca, pulgadas, estado, condicion, ubicacionTipo, desde, hasta),
                pageable);
        return new PaginaResponse<>(
                page.getContent().stream().map(this::aResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Búsqueda exacta para el escaneo de etiquetas: primero por código TK y, si no hay, por número de serie
     * (hay equipos cuyo "código" es la serie del fabricante).
     */
    @Transactional(readOnly = true)
    public EquipoResponse buscarPorCodigo(String valor) {
        String v = limpiar(valor);
        if (v == null) {
            throw ApiException.badRequest("Indica el código a buscar");
        }
        Optional<Equipo> porCodigo = equipoRepo.findByCodigoTkIgnoreCase(v);
        if (porCodigo.isPresent()) {
            return aResponse(porCodigo.get());
        }
        List<Equipo> porSerie = equipoRepo.findByNumeroSerieIgnoreCase(v);
        if (porSerie.size() == 1) {
            return aResponse(porSerie.get(0));
        }
        if (porSerie.size() > 1) {
            throw ApiException.conflicto("Hay varios equipos con la serie " + v + "; usa el código TK");
        }
        throw ApiException.noEncontrado("No existe un equipo con el código " + v);
    }

    @Transactional(readOnly = true)
    public EquipoResponse obtener(Long id) {
        return aResponse(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<String> listarMarcas() {
        return equipoRepo.listarMarcas();
    }

    @Transactional(readOnly = true)
    public List<String> listarPulgadas() {
        return equipoAtributoRepo.listarPulgadas();
    }

    @Transactional
    public Long crear(EquipoRequest req, String username) {
        String origen = limpiar(req.origenIngreso());
        return crearConEntrada(req, username, origen != null ? origen : "Registro en el sistema", null, null);
    }

    /**
     * Crea el equipo y su movimiento de entrada. El origen, la guía y las observaciones de esa entrada
     * se indican (por ejemplo, un traspaso desde el almacén M).
     */
    @Transactional
    public Long crearConEntrada(EquipoRequest req, String username, String origen, String guia,
                                String observaciones) {
        if (req.categoriaId() == null) {
            throw ApiException.badRequest("La categoría es obligatoria");
        }
        Categoria cat = categoriaRepo.findById(req.categoriaId())
                .filter(Categoria::getActivo)
                .orElseThrow(() -> ApiException.badRequest("La categoría no es válida"));

        String codigo = normalizarCodigo(req.codigoTk());
        if (equipoRepo.existsByCodigoTkIgnoreCase(codigo)) {
            throw ApiException.conflicto("Ya existe un equipo con el código " + codigo);
        }

        Equipo e = Equipo.builder().categoria(cat).build();
        copiarDatos(e, req);
        e.setCodigoTk(codigo);
        e = equipoRepo.save(e);
        sincronizarAtributos(e, req.atributos());

        Movimiento m = Movimiento.builder()
                .equipo(e)
                .tipo(TipoMovimiento.ENTRADA)
                .fecha(LocalDateTime.now())
                .origenDestino(origen)
                .guia(guia)
                .observaciones(observaciones)
                .usuario(usuarioRepo.findByUsername(username).orElse(null))
                .build();
        movimientoRepo.save(m);
        return e.getId();
    }

    @Transactional
    public void actualizar(Long id, EquipoRequest req) {
        Equipo e = buscar(id);
        if (req.categoriaId() != null && !req.categoriaId().equals(e.getCategoria().getId())) {
            throw ApiException.badRequest("No se puede cambiar la categoría de un equipo ya registrado");
        }
        String codigo = normalizarCodigo(req.codigoTk());
        if (equipoRepo.existsByCodigoTkIgnoreCaseAndIdNot(codigo, id)) {
            throw ApiException.conflicto("Ya existe otro equipo con el código " + codigo);
        }
        copiarDatos(e, req);
        e.setCodigoTk(codigo);
        sincronizarAtributos(e, req.atributos());
    }

    /**
     * Elimina un equipo (solo ADMIN, desde el controlador). Antes de borrarlo guarda una constancia con sus
     * datos, su historial completo, el motivo, quién lo eliminó y cuándo.
     */
    @Transactional
    public void eliminar(Long id, String motivo, String username) {
        String razon = limpiar(motivo);
        if (razon == null || razon.length() < 5) {
            throw ApiException.badRequest("Indica el motivo de la eliminación (mínimo 5 caracteres)");
        }
        if (razon.length() > 255) {
            throw ApiException.badRequest("El motivo es demasiado largo (máximo 255 caracteres)");
        }

        Equipo e = buscar(id);
        List<Movimiento> movimientos = movimientoRepo.findByEquipoIdOrderByFechaDescIdDesc(id);
        String quien = usuarioRepo.findByUsername(username).map(Usuario::getNombre).orElse(username);

        equipoEliminadoRepo.save(EquipoEliminado.builder()
                .equipoId(e.getId())
                .codigoTk(e.getCodigoTk())
                .categoria(e.getCategoria().getNombre())
                .numeroSerie(e.getNumeroSerie())
                .marca(e.getMarca())
                .descripcion(e.getDescripcion())
                .estado(e.getEstado().name())
                .ubicacion(textoUbicacion(e))
                .totalMovimientos(movimientos.size())
                .detalle(armarDetalle(e, movimientos))
                .motivo(razon)
                .eliminadoPor(quien)
                .fechaEliminacion(LocalDateTime.now())
                .build());

        movimientoRepo.deleteByEquipoId(id);
        equipoRepo.delete(e);
    }

    // ---------- utilidades ----------

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_DETALLE = 8000;

    private String valor(Object o) {
        if (o == null) return "—";
        String s = o.toString();
        return s.isBlank() ? "—" : s;
    }

    private String textoUbicacion(Equipo e) {
        String detalle = valor(e.getUbicacionDetalle());
        return switch (e.getUbicacionTipo()) {
            case ALMACEN_X -> "Almacén X";
            case CLIENTE -> "Cliente: " + detalle;
            case PERSONAL -> "Personal: " + detalle;
        };
    }

    /** Texto legible con todos los datos del equipo y su historial, para la constancia de eliminación. */
    private String armarDetalle(Equipo e, List<Movimiento> movimientos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Código: ").append(e.getCodigoTk()).append('\n');
        sb.append("Categoría: ").append(e.getCategoria().getNombre()).append('\n');
        sb.append("Marca: ").append(valor(e.getMarca())).append('\n');
        sb.append("Serie: ").append(valor(e.getNumeroSerie())).append('\n');
        sb.append("Descripción: ").append(valor(e.getDescripcion())).append('\n');
        e.getAtributos().stream()
                .sorted(Comparator.comparing(a -> a.getAtributo().getNombre()))
                .forEach(a -> sb.append(a.getAtributo().getNombre()).append(": ")
                        .append(a.getValor()).append('\n'));
        sb.append("Estado: ").append(e.getEstado()).append('\n');
        sb.append("Condición: ").append(e.getCondicion()).append('\n');
        sb.append("Ubicación: ").append(textoUbicacion(e)).append('\n');
        sb.append("Registrado el: ")
                .append(e.getCreatedAt() != null ? e.getCreatedAt().format(FORMATO_FECHA) : "—").append('\n');
        sb.append("Compra: ").append(valor(e.getFechaCompra()))
                .append(" | Proveedor: ").append(valor(e.getProveedor()))
                .append(" | Factura: ").append(valor(e.getFactura()))
                .append(" | Guía: ").append(valor(e.getGuia())).append('\n');
        sb.append("Costo sin IGV: US$ ").append(valor(e.getCostoUsd()))
                .append(" | S/ ").append(valor(e.getCostoPen()))
                .append(" | T.C. ").append(valor(e.getTipoCambio())).append('\n');
        sb.append("Garantía: ").append(valor(e.getGarantia())).append('\n');
        sb.append("Observaciones: ").append(valor(e.getObservaciones())).append('\n');

        sb.append("\nHistorial (").append(movimientos.size())
                .append(" movimientos, el más reciente primero):\n");
        for (Movimiento m : movimientos) {
            sb.append("- ").append(m.getFecha().format(FORMATO_FECHA)).append(" | ").append(m.getTipo());
            if (m.getOrigenDestino() != null) sb.append(" | ").append(m.getOrigenDestino());
            if (m.getOc() != null) sb.append(" | OC ").append(m.getOc());
            if (m.getRq() != null) sb.append(" | RQ ").append(m.getRq());
            if (m.getGuia() != null) sb.append(" | Guía ").append(m.getGuia());
            if (m.getFactura() != null) sb.append(" | Factura ").append(m.getFactura());
            if (m.getUsuario() != null) sb.append(" | por ").append(m.getUsuario().getNombre());
            if (Boolean.TRUE.equals(m.getAnulado())) {
                sb.append(" | ANULADO por ").append(valor(m.getAnuladoPor()))
                        .append(": ").append(valor(m.getMotivoAnulacion()));
            }
            sb.append('\n');
        }

        String texto = sb.toString();
        if (texto.length() > MAX_DETALLE) {
            texto = texto.substring(0, MAX_DETALLE - 60) + "\n… (detalle recortado por tamaño)";
        }
        return texto;
    }

    private Equipo buscar(Long id) {
        return equipoRepo.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el equipo con id " + id));
    }

    private String normalizarCodigo(String codigo) {
        String c = limpiar(codigo);
        if (c == null) throw ApiException.badRequest("El código es obligatorio");
        return c.toUpperCase();
    }

    private String limpiar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void copiarDatos(Equipo e, EquipoRequest req) {
        e.setNumeroSerie(limpiar(req.numeroSerie()));
        String marca = limpiar(req.marca());
        e.setMarca(marca == null ? null : marca.toUpperCase());
        e.setDescripcion(limpiar(req.descripcion()));
        e.setFechaCompra(req.fechaCompra());
        e.setProveedor(limpiar(req.proveedor()));
        e.setFactura(limpiar(req.factura()));
        e.setGuia(limpiar(req.guia()));
        e.setCostoUsd(req.costoUsd());
        e.setCostoPen(req.costoPen());
        e.setTipoCambio(req.tipoCambio());
        e.setGarantia(limpiar(req.garantia()));
        e.setObservaciones(limpiar(req.observaciones()));
        // Sin condición indicada: en un equipo nuevo queda "por revisar"; en una edición se conserva la actual
        if (req.condicion() != null) {
            e.setCondicion(req.condicion());
        }
    }

    private void sincronizarAtributos(Equipo e, Map<String, String> pedidos) {
        Map<String, String> deseados = pedidos == null ? Map.of() : pedidos;
        Long categoriaId = e.getCategoria().getId();

        Map<Long, EquipoAtributo> actuales = new HashMap<>();
        for (EquipoAtributo ea : e.getAtributos()) {
            actuales.put(ea.getAtributo().getId(), ea);
        }

        Set<Long> mantener = new HashSet<>();
        for (Map.Entry<String, String> entrada : deseados.entrySet()) {
            String valor = limpiar(entrada.getValue());
            if (valor == null) continue; // vacío = sin valor
            if (valor.length() > 100) {
                throw ApiException.badRequest("El valor de '" + entrada.getKey() + "' es demasiado largo");
            }
            CategoriaAtributo attr = atributoRepo
                    .findByCategoriaIdAndNombreIgnoreCase(categoriaId, entrada.getKey().trim())
                    .orElseThrow(() -> ApiException.badRequest("El atributo '" + entrada.getKey()
                            + "' no existe para la categoría " + e.getCategoria().getNombre()));
            mantener.add(attr.getId());

            EquipoAtributo existente = actuales.get(attr.getId());
            if (existente != null) {
                existente.setValor(valor);
            } else {
                e.getAtributos().add(EquipoAtributo.builder()
                        .id(new EquipoAtributo.EquipoAtributoId(e.getId(), attr.getId()))
                        .equipo(e)
                        .atributo(attr)
                        .valor(valor)
                        .build());
            }
        }
        e.getAtributos().removeIf(ea -> !mantener.contains(ea.getAtributo().getId()));
    }

    private EquipoResponse aResponse(Equipo e) {
        Map<String, String> atributos = new LinkedHashMap<>();
        e.getAtributos().stream()
                .sorted(Comparator.comparing(a -> a.getAtributo().getNombre()))
                .forEach(a -> atributos.put(a.getAtributo().getNombre(), a.getValor()));

        return new EquipoResponse(
                e.getId(), e.getCategoria().getId(), e.getCategoria().getNombre(),
                e.getCodigoTk(), e.getNumeroSerie(), e.getMarca(), e.getDescripcion(),
                e.getEstado().name(), e.getCondicion().name(), e.getUbicacionTipo().name(), e.getUbicacionDetalle(),
                e.getFechaCompra(), e.getProveedor(), e.getFactura(), e.getGuia(),
                e.getCostoUsd(), e.getCostoPen(), e.getTipoCambio(),
                e.getGarantia(), e.getObservaciones(), atributos,
                e.getCreatedAt(), e.getUpdatedAt());
    }
}