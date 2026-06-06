package com.cordillera.comisiones.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Vista de un usuario para el panel de administracion (solo la ve el admin).
 * No incluye la contrasena ni dato sensible: solo identidad y estadisticas
 * agregadas de su actividad.
 */
public record AdminUsuarioResponse(
    Long id,
    String email,
    String nombre,
    Instant creadoEn,
    long jornadas,
    BigDecimal totalAcumulado,
    LocalDate ultimaFecha,
    boolean esAdmin) {
}
