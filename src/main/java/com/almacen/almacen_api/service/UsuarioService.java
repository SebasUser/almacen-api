package com.almacen.almacen_api.service;

import com.almacen.almacen_api.dto.*;
import com.almacen.almacen_api.entity.Rol;
import com.almacen.almacen_api.entity.Usuario;
import com.almacen.almacen_api.exception.ApiException;
import com.almacen.almacen_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repo.findAllByOrderByNombreAsc().stream().map(this::aResponse).toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCrearRequest req) {
        String username = req.username().trim().toLowerCase();
        if (repo.existsByUsernameIgnoreCase(username)) {
            throw ApiException.conflicto("Ya existe un usuario con el nombre de usuario " + username);
        }
        Usuario u = Usuario.builder()
                .nombre(req.nombre().trim())
                .username(username)
                .password(encoder.encode(req.password()))
                .rol(req.rol())
                .activo(true)
                .build();
        return aResponse(repo.save(u));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioActualizarRequest req, String actor) {
        Usuario u = buscar(id);
        boolean esElMismo = u.getUsername().equalsIgnoreCase(actor);
        if (esElMismo && (u.getRol() != req.rol() || !Boolean.TRUE.equals(req.activo()))) {
            throw ApiException.badRequest("No puedes cambiar tu propio rol ni desactivar tu propia cuenta");
        }
        u.setNombre(req.nombre().trim());
        u.setRol(req.rol());
        u.setActivo(req.activo());

        // Corrección del nombre de usuario: debe seguir siendo único. El historial no se afecta porque
        // los movimientos guardan el id del usuario, no su nombre de usuario.
        String nuevoUsername = req.username() == null ? null : req.username().trim().toLowerCase();
        if (nuevoUsername != null && !nuevoUsername.isEmpty()
                && !nuevoUsername.equalsIgnoreCase(u.getUsername())) {
            if (repo.existsByUsernameIgnoreCase(nuevoUsername)) {
                throw ApiException.conflicto("Ya existe un usuario con el nombre de usuario " + nuevoUsername);
            }
            u.setUsername(nuevoUsername);
        }
        return aResponse(u);
    }

    /**
     * Transferencia guiada de la administración: la persona elegida pasa a ser ADMIN y, en la misma
     * operación, el administrador actual queda como USUARIO o desactivado. Se confirma con la contraseña
     * del administrador que entrega, y como el destino queda activo y ADMIN, nunca se queda el sistema sin admin.
     */
    @Transactional
    public UsuarioResponse transferirAdministracion(Long destinoId, TransferirAdminRequest.Accion accion,
                                                    String passwordActual, String actor) {
        Usuario yo = buscarPorUsername(actor);
        if (!encoder.matches(passwordActual, yo.getPassword())) {
            throw ApiException.badRequest("Tu contraseña no es correcta");
        }
        Usuario destino = buscar(destinoId);
        if (destino.getId().equals(yo.getId())) {
            throw ApiException.badRequest("Elige a otra persona: no puedes transferirte la administración a ti mismo");
        }
        if (!Boolean.TRUE.equals(destino.getActivo())) {
            throw ApiException.conflicto("La persona elegida está inactiva. Actívala primero");
        }

        destino.setRol(Rol.ADMIN);
        yo.setRol(Rol.USUARIO);
        if (accion == TransferirAdminRequest.Accion.DESACTIVAR) {
            yo.setActivo(false);
        }
        return aResponse(destino);
    }

    @Transactional
    public void restablecerPassword(Long id, String nuevaPassword) {
        Usuario u = buscar(id);
        u.setPassword(encoder.encode(nuevaPassword));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(String username) {
        return aResponse(buscarPorUsername(username));
    }

    @Transactional
    public void cambiarPassword(String username, String actual, String nueva) {
        Usuario u = buscarPorUsername(username);
        if (!encoder.matches(actual, u.getPassword())) {
            throw ApiException.badRequest("La contraseña actual no es correcta");
        }
        if (actual.equals(nueva)) {
            throw ApiException.badRequest("La nueva contraseña debe ser distinta de la actual");
        }
        u.setPassword(encoder.encode(nueva));
    }

    @Transactional
    public UsuarioResponse actualizarFoto(String username, String foto) {
        String f = foto == null ? "" : foto.trim();
        if (!f.matches("^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$")) {
            throw ApiException.badRequest("La imagen no es válida (usa JPG, PNG o WEBP)");
        }
        Usuario u = buscarPorUsername(username);
        u.setFoto(f);
        return aResponse(u);
    }

    @Transactional
    public UsuarioResponse quitarFoto(String username) {
        Usuario u = buscarPorUsername(username);
        u.setFoto(null);
        return aResponse(u);
    }

    private Usuario buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el usuario con id " + id));
    }

    private Usuario buscarPorUsername(String username) {
        return repo.findByUsername(username)
                .orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado"));
    }

    private UsuarioResponse aResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getUsername(),
                u.getRol().name(), u.getActivo(), u.getFoto());
    }
}