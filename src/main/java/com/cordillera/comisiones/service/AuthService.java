package com.cordillera.comisiones.service;

import com.cordillera.comisiones.domain.Usuario;
import com.cordillera.comisiones.repository.UsuarioRepository;
import com.cordillera.comisiones.security.JwtService;
import com.cordillera.comisiones.web.dto.AuthResponse;
import com.cordillera.comisiones.web.dto.LoginRequest;
import com.cordillera.comisiones.web.dto.RegisterRequest;
import java.time.Instant;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Registro y login: crea cuentas, valida contrasenas y emite el JWT. */
@Service
@Transactional
public class AuthService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final EmailDominioValidador dominioValidador;

    public AuthService(
        UsuarioRepository repo, PasswordEncoder encoder, JwtService jwt,
        EmailDominioValidador dominioValidador) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.dominioValidador = dominioValidador;
    }

    public AuthResponse registrar(RegisterRequest req) {
        String email = normalizar(req.email());
        if (!dominioValidador.dominioPuedeRecibirMail(email)) {
            throw new EmailInvalidoException();
        }
        if (repo.existsByEmail(email)) {
            throw new EmailYaRegistradoException(email);
        }
        Usuario u = Usuario.builder()
            .email(email)
            .passwordHash(encoder.encode(req.password()))
            .nombre(StringUtils.hasText(req.nombre()) ? req.nombre().trim() : null)
            .creadoEn(Instant.now())
            .build();
        u = repo.save(u);
        return aResponse(u);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = normalizar(req.email());
        Usuario u = repo.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Email o contrasena incorrectos"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new BadCredentialsException("Email o contrasena incorrectos");
        }
        return aResponse(u);
    }

    private AuthResponse aResponse(Usuario u) {
        return new AuthResponse(u.getId(), jwt.generar(u), u.getEmail(), u.getNombre());
    }

    private String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
