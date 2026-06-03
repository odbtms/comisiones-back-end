package com.cordillera.comisiones.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lo que el front envia para crear o actualizar una jornada.
 *
 * valorHora es opcional: si no viene, se usa el valor por defecto
 * configurado (app.calculo.valor-hora-default).
 */
public record JornadaRequest(
    @NotNull LocalDate fecha,

    @JsonFormat(pattern = "HH:mm") LocalTime entrada,

    @JsonFormat(pattern = "HH:mm") LocalTime salida,

    @PositiveOrZero BigDecimal valorHora,

    @NotNull @PositiveOrZero BigDecimal ventasBrutas,

    Boolean asistio,

    String nota) {}
