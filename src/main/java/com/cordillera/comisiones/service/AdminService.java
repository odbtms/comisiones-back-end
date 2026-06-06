package com.cordillera.comisiones.service;

import com.cordillera.comisiones.domain.Jornada;
import com.cordillera.comisiones.domain.Usuario;
import com.cordillera.comisiones.repository.JornadaRepository;
import com.cordillera.comisiones.repository.UsuarioRepository;
import com.cordillera.comisiones.web.dto.AdminUsuarioResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica del panel de administracion. Solo el usuario admin (por defecto el
 * id 1, configurable con app.admin.usuario-id) puede usarlo: cualquier otro
 * recibe AccesoDenegadoException (-> 403).
 *
 * Devuelve una vista de cada usuario con estadisticas agregadas (cantidad de
 * jornadas y total acumulado), sin exponer contrasenas ni datos sensibles.
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UsuarioRepository usuarios;
    private final JornadaRepository jornadas;
    private final CalculoComisionService calculo;
    private final Long adminId;

    public AdminService(
        UsuarioRepository usuarios,
        JornadaRepository jornadas,
        CalculoComisionService calculo,
        @Value("${app.admin.usuario-id:1}") Long adminId) {
        this.usuarios = usuarios;
        this.jornadas = jornadas;
        this.calculo = calculo;
        this.adminId = adminId;
    }

    /** True si ese usuario es el admin. */
    public boolean esAdmin(Long usuarioId) {
        return adminId.equals(usuarioId);
    }

    /** Lista todos los usuarios con sus estadisticas. Solo para el admin. */
    public List<AdminUsuarioResponse> listarUsuarios(Long solicitanteId) {
        if (!esAdmin(solicitanteId)) {
            throw new AccesoDenegadoException();
        }
        return usuarios.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
            .map(this::aResponse)
            .toList();
    }

    /**
     * Elimina un usuario y todas sus jornadas. Solo el admin puede; ademas no
     * puede eliminar SU PROPIA cuenta admin (evita quedarse sin acceso y perder
     * los datos heredados). Borra primero las jornadas (no hay cascade en la DB).
     */
    @Transactional
    public void eliminarUsuario(Long solicitanteId, Long objetivoId) {
        if (!esAdmin(solicitanteId)) {
            throw new AccesoDenegadoException();
        }
        if (esAdmin(objetivoId)) {
            throw new OperacionNoPermitidaException(
                "No podes eliminar la cuenta de administrador.");
        }
        if (!usuarios.existsById(objetivoId)) {
            throw new UsuarioNoEncontradoException(objetivoId);
        }
        jornadas.deleteByUsuarioId(objetivoId);
        usuarios.deleteById(objetivoId);
    }

    private AdminUsuarioResponse aResponse(Usuario u) {
        List<Jornada> js = jornadas.findByUsuarioId(u.getId());

        BigDecimal total = BigDecimal.ZERO;
        LocalDate ultima = null;
        for (Jornada j : js) {
            Calculo c = calculo.calcular(
                j.getEntrada(), j.getSalida(), j.getValorHora(),
                j.getVentasBrutas(), j.isAsistio());
            total = total.add(c.total());
            if (ultima == null || j.getFecha().isAfter(ultima)) {
                ultima = j.getFecha();
            }
        }

        return new AdminUsuarioResponse(
            u.getId(), u.getEmail(), u.getNombre(), u.getCreadoEn(),
            js.size(), total, ultima, esAdmin(u.getId()));
    }
}
