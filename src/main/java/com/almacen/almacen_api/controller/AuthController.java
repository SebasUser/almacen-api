package com.almacen.almacen_api.controller;

import com.almacen.almacen_api.dto.LoginRequest;
import com.almacen.almacen_api.dto.LoginResponse;
import com.almacen.almacen_api.entity.Usuario;
import com.almacen.almacen_api.repository.UsuarioRepository;
import com.almacen.almacen_api.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final UsuarioRepository repo;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        Usuario u = repo.findByUsername(req.username()).orElseThrow();
        return new LoginResponse(jwtService.generarToken(u), u.getUsername(),
                u.getNombre(), u.getRol().name());
    }
}