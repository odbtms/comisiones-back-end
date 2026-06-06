package com.cordillera.comisiones.service;

/**
 * Operación válida en estructura pero no permitida por una regla de negocio
 * (ej. el admin intentando eliminar su propia cuenta). Se traduce a 400.
 */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
