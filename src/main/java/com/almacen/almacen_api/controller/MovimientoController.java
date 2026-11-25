package com.almacen.almacen_api.controller;

import com.almacen.almacen_api.dto.AnularRequest;
import com.almacen.almacen_api.dto.MovimientoLoteRequest;
import com.almacen.almacen_api.dto.MovimientoRequest;
import com.almacen.almacen_api.dto.MovimientoResponse;
import com.almacen.almacen_api.dto.PaginaResponse;
import com.almacen.almacen_api.dto.UsuarioResumen;
import com.almacen.almacen_api.entity.TipoMovimiento;
import com.almacen.almacen_api.service.MovimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService service;

    @GetMapping("/equipos/{id}/movimientos")
    public List<MovimientoResponse> historial(@PathVariable Long id) {
        return service.historial(id);
    }

    @PostMapping("/equipos/{id}/movimientos")
    public ResponseEntity<MovimientoResponse> registrar(@PathVariable Long id,
                                                        @Valid @RequestBody MovimientoRequest req,
                                                        Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrar(id, req, auth.getName()));
    }

    @PostMapping("/movimientos/lote")
    public ResponseEntity<List<MovimientoResponse>> registrarLote(@Valid @RequestBody MovimientoLoteRequest req,
                                                                  Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarLote(req, auth.getName()));
    }

    @PostMapping("/movimientos/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public MovimientoResponse anular(@PathVariable Long id,
                                     @Valid @RequestBody AnularRequest req,
                                     Authentication auth) {
        return service.anular(id, req.motivo(), auth.getName());
    }

    @GetMapping("/movimientos")
    public PaginaResponse<MovimientoResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Boolean anulado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = {"fecha", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(q, tipo, usuarioId, anulado, desde, hasta, pageable);
    }

    /** Usuarios que han registrado movimientos, para el filtro "registrado por". */
    @GetMapping("/movimientos/usuarios")
    public List<UsuarioResumen> usuarios() {
        return service.usuariosConMovimientos();
    }
}
