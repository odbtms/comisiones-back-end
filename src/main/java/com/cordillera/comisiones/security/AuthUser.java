package com.cordillera.comisiones.security;

/**
 * Usuario autenticado que viaja en el SecurityContext.
 * Es lo que reciben los controllers con @AuthenticationPrincipal.
 */
public record AuthUser(Long id, String email, String nombre) {
}
