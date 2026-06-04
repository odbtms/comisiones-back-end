package com.cordillera.comisiones.service;

/** Se intento registrar un email que ya tiene cuenta. -> 409 */
public class EmailYaRegistradoException extends RuntimeException {
    public EmailYaRegistradoException(String email) {
        super("Ya existe una cuenta con el email " + email);
    }
}
