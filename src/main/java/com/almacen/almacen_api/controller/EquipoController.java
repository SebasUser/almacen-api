package com.almacen.almacen_api.controller;

import com.almacen.almacen_api.dto.EquipoRequest;
import com.almacen.almacen_api.dto.EquipoResponse;
import com.almacen.almacen_api.dto.PaginaResponse;
import com.almacen.almacen_api.entity.CondicionEquipo;
import com.almacen.almacen_api.entity.EstadoEquipo;
import com.almacen.almacen_api.entity.UbicacionTipo;
import com.almacen.almacen_api.service.EquipoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/equipos")
@RequiredArgsConstructor
public class EquipoController {

    private final EquipoService service;

    @GetMapping
    public PaginaResponse<EquipoResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String pulgadas,
            @RequestParam(required = false) EstadoEquipo estado,
            @RequestParam(required = false) CondicionEquipo condicion,
            @RequestParam(required = false) UbicacionTipo ubicacionTipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "codigoTk") Pageable pageable) {
        return service.listar(q, categoriaId, marca, pulgadas, estado, condicion, ubicacionTipo, desde, hasta, pageable);
    }

    @GetMapping("/marcas")
    public List<String> marcas() {
        return service.listarMarcas();
    }

    @GetMapping("/pulgadas")
    public List<String> pulgadas() {
        return service.listarPulgadas();
    }

    @GetMapping("/codigo")
    public EquipoResponse porCodigo(@RequestParam String valor) {
        return service.buscarPorCodigo(valor);
    }

    @GetMapping("/{id}")
    public EquipoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EquipoResponse> crear(@Valid @RequestBody EquipoRequest req,
                                                Authentication auth) {
        Long id = service.crear(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.obtener(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EquipoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EquipoRequest req) {
        service.actualizar(id, req);
        return service.obtener(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                        @RequestParam String motivo,
                                        Authentication auth) {
        service.eliminar(id, motivo, auth.getName());
        return ResponseEntity.noContent().build();
    }
}