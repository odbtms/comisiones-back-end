package com.cordillera.comisiones.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lo que el back devuelve por cada jornada: los datos de entrada
 * MAS todos los valores calculados, listos para mostrar.
 */
public record JornadaResponse(
    Long id,
    LocalDate fecha,
    @JsonFormat(pattern = "HH:mm") LocalTime entrada,
    @JsonFormat(pattern = "HH:mm") LocalTime salida,
    BigDecimal valorHora,
    BigDecimal ventasBrutas,
    boolean asistio,
    String nota,
    // ---- valores calculados ----
    BigDecimal horas,
    BigDecimal pagoBase,
    BigDecimal ventasNetas,
    BigDecimal comision,
    BigDecimal total) {}
