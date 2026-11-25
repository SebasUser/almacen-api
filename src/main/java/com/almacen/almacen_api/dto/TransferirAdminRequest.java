package com.almacen.almacen_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransferirAdminRequest(
        @NotNull(message = "Indica qué pasará con tu cuenta") Accion accion,
        @NotBlank(message = "Escribe tu contraseña para confirmar") String passwordActual
) {
    /** Qué pasa con la cuenta del administrador que entrega la administración. */
    public enum Accion { DEJAR_COMO_USUARIO, DESACTIVAR }
}
