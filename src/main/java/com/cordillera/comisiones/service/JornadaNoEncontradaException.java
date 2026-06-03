package com.cordillera.comisiones.service;

/** Se lanza cuando se pide una jornada que no existe. Mapea a HTTP 404. */
public class JornadaNoEncontradaException extends RuntimeException {
    public JornadaNoEncontradaException(Long id) {
        super("No existe la jornada con id " + id);
    }
}
