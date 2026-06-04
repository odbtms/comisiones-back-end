package com.cordillera.comisiones.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para crear una cuenta nueva. */
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 100, message = "La contrasena debe tener al menos 6 caracteres")
    String password,
    @Size(max = 80) String nombre) {
}
