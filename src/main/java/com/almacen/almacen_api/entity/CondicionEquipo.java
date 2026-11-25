package com.almacen.almacen_api.entity;

/** Condición física/operativa del equipo; es independiente de su estado (disponible, alquilado, etc.). */
public enum CondicionEquipo {
    OPERATIVO,      // en buen estado: sirve para alquiler, venta o asignación
    SOLO_VENTA,     // funciona pero no es apto para alquiler (sin cargador, pantalla opaca, etc.)
    NO_OPERATIVO,   // con falla grave: no se alquila, asigna ni vende hasta repararlo o reclasificarlo
    POR_REVISAR     // aún sin evaluar
}
