package com.sazloop.reservas.repository;

import com.sazloop.reservas.domain.RetencionStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface RetencionStockRepository extends JpaRepository<RetencionStock, Long> {
    boolean existsByConsumidor_IdAndEstadoAndFechaExpiracionAfter(
            Long consumidorId, RetencionStock.EstadoRetencion estado, java.time.LocalDateTime ahora);
    List<RetencionStock> findByReserva_Id(Long reservaId);
    List<RetencionStock> findByEstadoAndFechaExpiracionLessThanEqual(
            RetencionStock.EstadoRetencion estado, LocalDateTime fecha);
}
