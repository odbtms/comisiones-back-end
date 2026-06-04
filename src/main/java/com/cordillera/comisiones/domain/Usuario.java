package com.cordillera.comisiones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una cuenta de la app. Cada usuario ve solo SUS jornadas
 * (las jornadas referencian su id por la columna usuario_id).
 *
 * Guarda el email (identificador de login) y la contrasena YA HASHEADA
 * con BCrypt; la contrasena en texto plano nunca se persiste.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador de login. Unico, se guarda en minuscula. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt de la contrasena (nunca el texto plano). */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Nombre para mostrar (opcional). */
    private String nombre;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;
}
