package com.sazloop.reservas.repository;

import com.sazloop.reservas.domain.CodigoRecojo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CodigoRecojoRepository extends JpaRepository<CodigoRecojo, Long> {
    /** Busca ignorando guiones, espacios y mayúsculas: "abbafe9e4869" equivale a "ABBAFE9E-4869-4A". */
    @Query("select c from CodigoRecojo c where upper(replace(c.codigo, '-', '')) = :codigo")
    Optional<CodigoRecojo> findByCodigo(@Param("codigo") String codigo);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CodigoRecojo c where upper(replace(c.codigo, '-', '')) = :codigo")
    Optional<CodigoRecojo> bloquear(@Param("codigo") String codigo);

    /** Quita espacios y guiones y pasa a mayúsculas, igual que se normaliza el código guardado. */
    static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.replaceAll("[\s-]", "").toUpperCase(java.util.Locale.ROOT);
    }
}
