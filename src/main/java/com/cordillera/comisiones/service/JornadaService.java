package com.cordillera.comisiones.service;

import com.cordillera.comisiones.domain.Jornada;
import com.cordillera.comisiones.repository.JornadaRepository;
import com.cordillera.comisiones.web.dto.JornadaRequest;
import com.cordillera.comisiones.web.dto.JornadaResponse;
import com.cordillera.comisiones.web.dto.ResumenMensualResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JornadaService {

    private final JornadaRepository repo;
    private final CalculoComisionService calculo;
    private final BigDecimal valorHoraDefault;

    public JornadaService(
        JornadaRepository repo,
        CalculoComisionService calculo,
        @Value("${app.calculo.valor-hora-default:3098}") BigDecimal valorHoraDefault) {
        this.repo = repo;
        this.calculo = calculo;
        this.valorHoraDefault = valorHoraDefault;
    }

    public JornadaResponse crear(Long usuarioId, JornadaRequest req) {
        Jornada j = new Jornada();
        j.setUsuarioId(usuarioId);
        aplicar(j, req);
        return aResponse(repo.save(j));
    }

    public JornadaResponse actualizar(Long usuarioId, Long id, JornadaRequest req) {
        Jornada j = repo.findByIdAndUsuarioId(id, usuarioId)
            .orElseThrow(() -> new JornadaNoEncontradaException(id));
        aplicar(j, req);
        return aResponse(repo.save(j));
    }

    public void eliminar(Long usuarioId, Long id) {
        if (!repo.existsByIdAndUsuarioId(id, usuarioId)) {
            throw new JornadaNoEncontradaException(id);
        }
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public JornadaResponse obtener(Long usuarioId, Long id) {
        return repo.findByIdAndUsuarioId(id, usuarioId)
            .map(this::aResponse)
            .orElseThrow(() -> new JornadaNoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public ResumenMensualResponse resumenMensual(Long usuarioId, int anio, int mes) {
        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate desde = ym.atDay(1);
        LocalDate hasta = ym.atEndOfMonth();

        List<JornadaResponse> jornadas = repo
            .findByUsuarioIdAndFechaBetweenOrderByFecha(usuarioId, desde, hasta)
            .stream()
            .map(this::aResponse)
            .toList();

        BigDecimal totalHoras = BigDecimal.ZERO;
        BigDecimal totalVentasBrutas = BigDecimal.ZERO;
        BigDecimal totalPagoBase = BigDecimal.ZERO;
        BigDecimal totalComision = BigDecimal.ZERO;
        BigDecimal totalGeneral = BigDecimal.ZERO;
        // Como un dia puede tener varios tramos (turno partido), contamos
        // FECHAS distintas trabajadas, no la cantidad de registros.
        Set<LocalDate> fechasTrabajadas = new HashSet<>();

        for (JornadaResponse r : jornadas) {
            totalPagoBase = totalPagoBase.add(r.pagoBase());
            totalComision = totalComision.add(r.comision());
            totalGeneral = totalGeneral.add(r.total());
            if (r.ventasBrutas() != null) {
                totalVentasBrutas = totalVentasBrutas.add(r.ventasBrutas());
            }
            if (r.asistio()) {
                totalHoras = totalHoras.add(r.horas());
                fechasTrabajadas.add(r.fecha());
            }
        }

        return new ResumenMensualResponse(
            anio, mes, fechasTrabajadas.size(),
            totalHoras, totalVentasBrutas, totalPagoBase, totalComision, totalGeneral,
            jornadas);
    }

    // ---- helpers ----

    private void aplicar(Jornada j, JornadaRequest req) {
        j.setFecha(req.fecha());
        j.setEntrada(req.entrada());
        j.setSalida(req.salida());
        j.setValorHora(req.valorHora() != null ? req.valorHora() : valorHoraDefault);
        j.setVentasBrutas(req.ventasBrutas());
        j.setAsistio(req.asistio() == null || req.asistio());
        j.setNota(req.nota());
    }

    private JornadaResponse aResponse(Jornada j) {
        Calculo c = calculo.calcular(
            j.getEntrada(), j.getSalida(), j.getValorHora(),
            j.getVentasBrutas(), j.isAsistio());
        return new JornadaResponse(
            j.getId(), j.getFecha(), j.getEntrada(), j.getSalida(),
            j.getValorHora(), j.getVentasBrutas(), j.isAsistio(), j.getNota(),
            c.horas(), c.pagoBase(), c.ventasNetas(), c.comision(), c.total());
    }
}
