package com.cordillera.comisiones.repository;

import com.cordillera.comisiones.domain.Jornada;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JornadaRepository extends JpaRepository<Jornada, Long> {

    List<Jornada> findByUsuarioIdAndFechaBetweenOrderByFecha(
        Long usuarioId, LocalDate desde, LocalDate hasta);

    /** Todas las jornadas de un usuario (para estadisticas del panel admin). */
    List<Jornada> findByUsuarioId(Long usuarioId);

    /** Borra todas las jornadas de un usuario (al eliminar la cuenta). */
    void deleteByUsuarioId(Long usuarioId);

    /** Busca una jornada solo si pertenece al usuario (evita tocar la de otro). */
    Optional<Jornada> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);
}
