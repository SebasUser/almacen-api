package com.almacen.almacen_api.controller;

import com.almacen.almacen_api.dto.IngresoLoteRequest;
import com.almacen.almacen_api.dto.IngresoLoteResponse;
import com.almacen.almacen_api.service.IngresoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingresos")
@RequiredArgsConstructor
public class IngresoController {

    private final IngresoService service;

    @PostMapping("/lote")
    public ResponseEntity<IngresoLoteResponse> ingresarLote(@Valid @RequestBody IngresoLoteRequest req,
                                                            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ingresar(req, auth.getName()));
    }
}
