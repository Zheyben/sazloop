package com.sazloop.experiencia.service;

import com.sazloop.experiencia.domain.ImpactoCO2Evitado;
import com.sazloop.experiencia.repository.ImpactoCO2EvitadoRepository;
import com.sazloop.ofertas.domain.VariantePack.TamanioVariante;
import com.sazloop.reservas.domain.DetalleReserva;
import com.sazloop.reservas.domain.Reserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Impacto ambiental de cada entrega (RF-26): kilos de comida recuperada y CO2 evitado.
 *
 * <p>Los pesos por tamaño son una suposición del proyecto. El factor de 2,5 kg de CO2 equivalente
 * por kilo de comida sale de FAO (2013), «Food Wastage Footprint: Impacts on Natural Resources»:
 * unos 3.300 millones de toneladas de CO2e por unos 1.300 millones de toneladas de comida
 * desperdiciada al año (3,3 / 1,3, aproximadamente 2,5).
 */
@Service
public class ImpactoService {
    /** Peso estimado por unidad de pack, según su tamaño. */
    static final Map<TamanioVariante, BigDecimal> KG_POR_TAMANIO = Map.of(
            TamanioVariante.PEQUENO, new BigDecimal("0.5"),
            TamanioVariante.MEDIANO, new BigDecimal("1.0"),
            TamanioVariante.GRANDE, new BigDecimal("1.5"));
    /** Si aparece un tamaño que no está en la tabla, se usa 1,0 kg. */
    static final BigDecimal KG_POR_DEFECTO = new BigDecimal("1.0");
    static final BigDecimal KG_CO2_POR_KG = new BigDecimal("2.5");
    static final String METODOLOGIA = "Peso estimado por tamaño (pequeño 0,5 kg, mediano 1 kg, grande 1,5 kg) "
            + "× unidades; CO₂ = kg × 2,5 (FAO, 2013, Food Wastage Footprint)";

    private final ImpactoCO2EvitadoRepository impactos;

    public ImpactoService(ImpactoCO2EvitadoRepository impactos) { this.impactos = impactos; }

    /** Se usa en toda entrega (recojo, efectivo presencial y delivery): guarda kilos, CO2 y metodología. */
    @Transactional
    public ImpactoCO2Evitado registrar(Reserva reserva) {
        BigDecimal kilos = BigDecimal.ZERO;
        for (DetalleReserva detalle : reserva.getDetalles()) {
            BigDecimal peso = detalle.getVariante().getTamanio() == null ? KG_POR_DEFECTO
                    : KG_POR_TAMANIO.getOrDefault(detalle.getVariante().getTamanio(), KG_POR_DEFECTO);
            kilos = kilos.add(peso.multiply(BigDecimal.valueOf(detalle.getCantidad())));
        }
        return impactos.save(ImpactoCO2Evitado.builder().recojo(reserva.getRecojo())
                .alimentoRecuperadoKg(kilos).co2EvitadoKg(kilos.multiply(KG_CO2_POR_KG))
                .estimacionDisponible(true).metodologia(METODOLOGIA).build());
    }
}
