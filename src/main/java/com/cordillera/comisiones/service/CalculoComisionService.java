package com.cordillera.comisiones.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Corazon del sistema: replica EXACTAMENTE las formulas del Excel manual.
 *
 * Por cada dia:
 *   horas        = (salida - entrada) en horas
 *   pagoBase     = horas * valorHora
 *   ventasNetas  = REDONDEAR(ventasBrutas / (1 + IVA))     -> saca el IVA
 *   comision     = ventasNetas * %comision
 *   total        = REDONDEAR(pagoBase + comision)
 *
 * Las horas salen de (salida - entrada). Regla de colacion: si la jornada
 * supera el umbral (por defecto 8 h) se descuenta 1 h, asi no se paga el
 * almuerzo. Ambos valores son configurables.
 *
 * Si no asistio ("no fui a trabajar"), el dia paga 0.
 *
 * El redondeo HALF_UP imita la funcion REDONDEAR/ROUND de Excel.
 */
@Service
public class CalculoComisionService {

    private final BigDecimal factorIva;        // 1 + IVA, ej: 1.19
    private final BigDecimal pctComision;      // ej: 0.03
    private final BigDecimal colacionUmbral;   // horas a partir de las cuales descuenta
    private final BigDecimal colacionDescuento; // horas que se descuentan

    public CalculoComisionService(
        @Value("${app.calculo.iva:0.19}") BigDecimal iva,
        @Value("${app.calculo.comision:0.03}") BigDecimal comision,
        @Value("${app.calculo.colacion.umbral-horas:8}") BigDecimal colacionUmbral,
        @Value("${app.calculo.colacion.descuento-horas:1}") BigDecimal colacionDescuento) {
        this.factorIva = BigDecimal.ONE.add(iva);
        this.pctComision = comision;
        this.colacionUmbral = colacionUmbral;
        this.colacionDescuento = colacionDescuento;
    }

    public Calculo calcular(
        LocalTime entrada,
        LocalTime salida,
        BigDecimal valorHora,
        BigDecimal ventasBrutas,
        boolean asistio) {

        BigDecimal ventas = ventasBrutas == null ? BigDecimal.ZERO : ventasBrutas;
        BigDecimal valor = valorHora == null ? BigDecimal.ZERO : valorHora;

        BigDecimal horas = horasEntre(entrada, salida);
        BigDecimal ventasNetas = ventas.divide(factorIva, 0, RoundingMode.HALF_UP);

        if (!asistio) {
            // No fui a trabajar: no se gana nada ese dia.
            return new Calculo(horas, ceros(), ventasNetas, ceros(), ceros());
        }

        BigDecimal pagoBase = horas.multiply(valor);
        BigDecimal comision = ventasNetas.multiply(pctComision);
        BigDecimal total = pagoBase.add(comision).setScale(0, RoundingMode.HALF_UP);

        return new Calculo(horas, pagoBase, ventasNetas, comision, total);
    }

    /**
     * Horas decimales entre entrada y salida (0 si falta alguna), aplicando
     * el descuento de colacion cuando la jornada supera el umbral.
     */
    private BigDecimal horasEntre(LocalTime entrada, LocalTime salida) {
        if (entrada == null || salida == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        long minutos = Duration.between(entrada, salida).toMinutes();
        if (minutos < 0) {
            minutos = 0;
        }
        BigDecimal horas = BigDecimal.valueOf(minutos)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        // Colacion: si trabaja mas del umbral, se descuenta 1 h (no se paga almuerzo).
        if (horas.compareTo(colacionUmbral) > 0) {
            horas = horas.subtract(colacionDescuento);
        }
        return horas;
    }

    private BigDecimal ceros() {
        return BigDecimal.ZERO.setScale(0);
    }
}
