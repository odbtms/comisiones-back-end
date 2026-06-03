package com.cordillera.comisiones.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen de un mes: todas las jornadas + los totales,
 * equivalente a la hoja completa del Excel con su TOTAL GENERAL.
 */
public record ResumenMensualResponse(
    int anio,
    int mes,
    int diasTrabajados,
    BigDecimal totalHoras,
    BigDecimal totalPagoBase,
    BigDecimal totalComision,
    BigDecimal totalGeneral,
    List<JornadaResponse> jornadas) {}
