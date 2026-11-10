package com.sazloop.experiencia.repository;

import com.sazloop.experiencia.domain.ImpactoCO2Evitado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImpactoCO2EvitadoRepository extends JpaRepository<ImpactoCO2Evitado, Long> {
    boolean existsByRecojo_Id(Long recojoId);

    /** Impactos de las entregas de un local (para sumar kilos y CO2 en el portal de empresa). */
    @org.springframework.data.jpa.repository.Query(
            "select i from ImpactoCO2Evitado i where i.recojo.reserva.oferta.packSorpresa.establecimiento.id = :localId")
    java.util.List<ImpactoCO2Evitado> findByLocal(@org.springframework.data.repository.query.Param("localId") Long localId);
}
