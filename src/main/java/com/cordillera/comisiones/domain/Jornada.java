package com.cordillera.comisiones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una jornada = el registro de un dia de trabajo.
 *
 * Guarda solo los DATOS DE ENTRADA (lo que el usuario marca/escribe).
 * Los valores derivados (horas, pago base, venta neta, comision, total)
 * NO se guardan: se calculan al vuelo con {@code CalculoComisionService},
 * asi nunca quedan desincronizados si cambia una regla.
 */
@Entity
@Table(
    name = "jornada",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_jornada_usuario_fecha",
        columnNames = {"usuario_id", "fecha"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Jornada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Dueno del registro. Por ahora hay un solo usuario (=1).
     * Lo dejamos preparado para multiusuario en la Fase 3.
     */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDate fecha;

    private LocalTime entrada;

    private LocalTime salida;

    /** Valor de la hora vigente ese dia (cambia mes a mes). */
    @Column(name = "valor_hora", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorHora;

    /** Ventas brutas del dia, con IVA incluido. */
    @Column(name = "ventas_brutas", nullable = false, precision = 14, scale = 2)
    private BigDecimal ventasBrutas;

    /**
     * Si es false significa "no fui a trabajar": el dia paga 0,
     * aunque tenga un horario precargado.
     */
    @Column(nullable = false)
    private boolean asistio;

    /** Nota opcional, ej: "feriado", "licencia". */
    private String nota;
}
