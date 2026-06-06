package com.cordillera.comisiones.web;

import com.cordillera.comisiones.security.AuthUser;
import com.cordillera.comisiones.service.AdminService;
import com.cordillera.comisiones.web.dto.AdminUsuarioResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Panel de administracion: solo el usuario admin (id 1 por defecto) lo usa.
 * La autorizacion se valida en AdminService (403 si no es admin); estar
 * autenticado ya lo exige SecurityConfig para todo /api/**.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    /** Lista de usuarios con estadisticas. 403 si el que pregunta no es admin. */
    @GetMapping("/usuarios")
    public List<AdminUsuarioResponse> usuarios(@AuthenticationPrincipal AuthUser user) {
        return service.listarUsuarios(user.id());
    }

    /**
     * Elimina un usuario y sus jornadas (solo admin). 403 si no es admin,
     * 400 si intenta borrar la cuenta admin, 404 si el usuario no existe.
     */
    @DeleteMapping("/usuarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
        @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        service.eliminarUsuario(user.id(), id);
    }
}
