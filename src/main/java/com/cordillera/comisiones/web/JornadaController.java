package com.cordillera.comisiones.web;

import com.cordillera.comisiones.service.JornadaService;
import com.cordillera.comisiones.web.dto.JornadaRequest;
import com.cordillera.comisiones.web.dto.JornadaResponse;
import com.cordillera.comisiones.web.dto.ResumenMensualResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de jornadas. El front (React) consume estos endpoints para
 * cargar dias y ver el resumen mensual.
 */
@RestController
@RequestMapping("/api/jornadas")
public class JornadaController {

    private final JornadaService service;

    public JornadaController(JornadaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JornadaResponse crear(@Valid @RequestBody JornadaRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    public JornadaResponse actualizar(
        @PathVariable Long id, @Valid @RequestBody JornadaRequest req) {
        return service.actualizar(id, req);
    }

    @GetMapping("/{id}")
    public JornadaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    /** Resumen de un mes: todas las jornadas + totales (el "TOTAL GENERAL" del Excel). */
    @GetMapping("/resumen")
    public ResumenMensualResponse resumen(
        @RequestParam int anio, @RequestParam int mes) {
        return service.resumenMensual(anio, mes);
    }
}
