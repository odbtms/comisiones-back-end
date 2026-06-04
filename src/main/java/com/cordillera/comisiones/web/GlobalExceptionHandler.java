package com.cordillera.comisiones.web;

import com.cordillera.comisiones.service.EmailYaRegistradoException;
import com.cordillera.comisiones.service.JornadaNoEncontradaException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones a respuestas HTTP claras para el front.
 *   - jornada inexistente    -> 404
 *   - body invalido (@Valid)  -> 400 con el detalle por campo
 *   - email ya registrado / fecha duplicada -> 409
 *   - email o contrasena mal  -> 401
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JornadaNoEncontradaException.class)
    public ProblemDetail noEncontrada(JornadaNoEncontradaException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Jornada no encontrada");
        return pd;
    }

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ProblemDetail emailDuplicado(EmailYaRegistradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Email en uso");
        return pd;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail credencialesInvalidas(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED, "Email o contrasena incorrectos.");
        pd.setTitle("Credenciales invalidas");
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflictoDatos(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "Ya existe un registro con esos datos (¿fecha repetida?).");
        pd.setTitle("Conflicto");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalida(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Hay datos invalidos en la jornada.");
        pd.setTitle("Validacion fallida");
        pd.setProperty("errores", errores);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
