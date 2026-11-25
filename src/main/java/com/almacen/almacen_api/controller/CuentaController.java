package com.almacen.almacen_api.controller;

import com.almacen.almacen_api.dto.CambiarPasswordRequest;
import com.almacen.almacen_api.dto.FotoRequest;
import com.almacen.almacen_api.dto.UsuarioResponse;
import com.almacen.almacen_api.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cuenta")
@RequiredArgsConstructor
public class CuentaController {

    private final UsuarioService service;

    @GetMapping("/me")
    public UsuarioResponse perfil(Authentication auth) {
        return service.perfil(auth.getName());
    }

    @PutMapping("/foto")
    public UsuarioResponse subirFoto(@Valid @RequestBody FotoRequest req, Authentication auth) {
        return service.actualizarFoto(auth.getName(), req.foto());
    }

    @DeleteMapping("/foto")
    public UsuarioResponse quitarFoto(Authentication auth) {
        return service.quitarFoto(auth.getName());
    }

    @PostMapping("/password")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest req,
                                                Authentication auth) {
        service.cambiarPassword(auth.getName(), req.passwordActual(), req.passwordNueva());
        return ResponseEntity.noContent().build();
    }
}