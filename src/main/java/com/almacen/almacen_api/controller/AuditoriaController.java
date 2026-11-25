package com.almacen.almacen_api.controller;

import com.almacen.almacen_api.dto.EquipoEliminadoResponse;
import com.almacen.almacen_api.dto.PaginaResponse;
import com.almacen.almacen_api.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService service;

    @GetMapping("/equipos-eliminados")
    public PaginaResponse<EquipoEliminadoResponse> equiposEliminados(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "fechaEliminacion", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listarEliminados(q, pageable);
    }
}
