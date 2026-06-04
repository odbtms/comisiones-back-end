package com.cordillera.comisiones.security;

import com.cordillera.comisiones.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Firma y valida los JWT del login (HS256).
 * El secreto y la expiracion vienen por config (JWT_SECRET / app.jwt.*).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiracionMs;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-ms:604800000}") long expiracionMs) {
        // HS256 exige una clave de al menos 256 bits (32 bytes).
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    /** Genera un token para el usuario (sub = email, claim uid = id). */
    public String generar(Usuario u) {
        Instant ahora = Instant.now();
        return Jwts.builder()
            .subject(u.getEmail())
            .claim("uid", u.getId())
            .claim("nombre", u.getNombre())
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(ahora.plusMillis(expiracionMs)))
            .signWith(key)
            .compact();
    }

    /** Valida la firma/expiracion y devuelve los claims. Lanza JwtException si es invalido. */
    public Jws<Claims> parsear(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
