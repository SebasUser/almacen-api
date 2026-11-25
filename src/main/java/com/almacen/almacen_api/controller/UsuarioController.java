package com.almacen.almacen_api.controller;

import com.almacen.almacen_api.dto.*;
import com.almacen.almacen_api.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioCrearRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody UsuarioActualizarRequest req,
                                      Authentication auth) {
        return service.actualizar(id, req, auth.getName());
    }

    @PostMapping("/{id}/transferir-administracion")
    public UsuarioResponse transferirAdministracion(@PathVariable Long id,
                                                    @Valid @RequestBody TransferirAdminRequest req,
                                                    Authentication auth) {
        return service.transferirAdministracion(id, req.accion(), req.passwordActual(), auth.getName());
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> restablecerPassword(@PathVariable Long id,
                                                    @Valid @RequestBody PasswordResetRequest req) {
        service.restablecerPassword(id, req.nuevaPassword());
        return ResponseEntity.noContent().build();
    }
}