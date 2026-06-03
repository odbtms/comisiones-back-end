package com.cordillera.comisiones.service;

import java.math.BigDecimal;

/**
 * Resultado del calculo de un dia. Todos los valores son derivados,
 * no se persisten.
 *
 * @param horas        horas trabajadas (salida - entrada)
 * @param pagoBase     horas * valorHora
 * @param ventasNetas  ventas brutas sin IVA, redondeadas
 * @param comision     ventasNetas * porcentaje de comision
 * @param total        pagoBase + comision, redondeado (0 si no asistio)
 */
public record Calculo(
    BigDecimal horas,
    BigDecimal pagoBase,
    BigDecimal ventasNetas,
    BigDecimal comision,
    BigDecimal total) {}
