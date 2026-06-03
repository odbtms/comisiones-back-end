package com.cordillera.comisiones.repository;

import com.cordillera.comisiones.domain.Jornada;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JornadaRepository extends JpaRepository<Jornada, Long> {

    List<Jornada> findByUsuarioIdAndFechaBetweenOrderByFecha(
        Long usuarioId, LocalDate desde, LocalDate hasta);

    Optional<Jornada> findByUsuarioIdAndFecha(Long usuarioId, LocalDate fecha);
}
