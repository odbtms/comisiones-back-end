package com.cordillera.comisiones.service;

/**
 * Se lanza cuando un usuario autenticado intenta una accion para la que no
 * tiene permiso (ej. acceder al panel de administracion sin ser admin).
 * El GlobalExceptionHandler la traduce a 403.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException() {
        super("No tenes permisos para acceder a esta seccion.");
    }
}
