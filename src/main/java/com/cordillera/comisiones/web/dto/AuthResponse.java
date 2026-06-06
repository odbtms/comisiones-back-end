package com.cordillera.comisiones.web.dto;

/** Respuesta del login/registro: el token y datos basicos del usuario. */
public record AuthResponse(Long id, String token, String email, String nombre) {
}
