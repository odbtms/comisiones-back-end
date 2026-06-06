package com.cordillera.comisiones.service;

/**
 * El email tiene formato válido pero su dominio no puede recibir correos
 * (no existe o no tiene servidor de mail). Se traduce a 400.
 */
public class EmailInvalidoException extends RuntimeException {

    public EmailInvalidoException() {
        super("El correo no parece valido: revisá que el dominio esté bien escrito.");
    }
}
