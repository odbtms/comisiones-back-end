package com.cordillera.comisiones.web;

import com.cordillera.comisiones.security.AuthUser;
import com.cordillera.comisiones.service.AuthService;
import com.cordillera.comisiones.web.dto.AuthResponse;
import com.cordillera.comisiones.web.dto.LoginRequest;
import com.cordillera.comisiones.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro / login. Estos endpoints son publicos (ver SecurityConfig);
 * el resto de la API exige el token que devuelven aca.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrar(@Valid @RequestBody RegisterRequest req) {
        return service.registrar(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    /** Datos del usuario logueado (para que el front valide el token al arrancar). */
    @GetMapping("/me")
    public Map<String, Object> yo(@AuthenticationPrincipal AuthUser user) {
        return Map.of("id", user.id(), "email", user.email(),
            "nombre", user.nombre() == null ? "" : user.nombre());
    }
}
