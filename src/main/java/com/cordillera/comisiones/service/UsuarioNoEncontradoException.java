package com.cordillera.comisiones.service;

/** El usuario pedido no existe. El GlobalExceptionHandler la traduce a 404. */
public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(Long id) {
        super("No existe el usuario con id " + id + ".");
    }
}
