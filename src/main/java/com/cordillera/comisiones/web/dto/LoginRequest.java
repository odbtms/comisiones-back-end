package com.cordillera.comisiones.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Datos para iniciar sesion. */
public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password) {
}
