package com.sazloop.identidad.service;

import com.sazloop.experiencia.domain.Resena;
import com.sazloop.experiencia.domain.ImpactoCO2Evitado;
import com.sazloop.experiencia.repository.ImpactoCO2EvitadoRepository;
import com.sazloop.experiencia.repository.ResenaRepository;
import com.sazloop.identidad.dto.EstablecimientoResponse;
import com.sazloop.reservas.domain.DetalleReserva;
import com.sazloop.reservas.domain.Reserva;
import com.sazloop.reservas.repository.ReservaRepository;
import com.sazloop.transacciones.domain.Pago;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Indicadores por local y consolidados, calculados de los registros existentes. */
@Service
public class IndicadoresEmpresaService {
    private final EmpresaService empresas;
    private final ReservaRepository reservas;
    private final ResenaRepository resenas;
    private final ImpactoCO2EvitadoRepository impactos;

    public IndicadoresEmpresaService(EmpresaService empresas, ReservaRepository reservas,
            ResenaRepository resenas, ImpactoCO2EvitadoRepository impactos) {
        this.empresas = empresas; this.reservas = reservas; this.resenas = resenas; this.impactos = impactos;
    }

    @Transactional(readOnly = true)
    public Resumen resumen(Long usuarioId) {
        List<Indicadores> locales = new ArrayList<>();
        for (EstablecimientoResponse local : empresas.listarEstablecimientos(usuarioId)) {
            locales.add(calcular(local.id(), local.nombre(),
                    reservas.findByOferta_PackSorpresa_Establecimiento_Id(local.id()),
                    resenas.findByEstablecimiento_IdAndVisibleTrue(local.id()),
                    impactos.findByLocal(local.id())));
        }
        return new Resumen(locales, consolidar(locales));
    }

    /** Suma los indicadores de todos los locales en una sola fila «Total empresa». */
    private Indicadores consolidar(List<Indicadores> locales) {
        int ventas = 0;
        int cancelaciones = 0;
        int noRecogidas = 0;
        int entregados = 0;
        int numResenas = 0;
        BigDecimal importeVentas = BigDecimal.ZERO;
        BigDecimal comisiones = BigDecimal.ZERO;
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal sumaNotas = BigDecimal.ZERO;
        BigDecimal kilos = BigDecimal.ZERO;
        BigDecimal co2 = BigDecimal.ZERO;
        for (Indicadores local : locales) {
            kilos = kilos.add(local.alimentoRecuperadoKg());
            co2 = co2.add(local.co2EvitadoKg());
            ventas += local.ventas();
            cancelaciones += local.cancelaciones();
            noRecogidas += local.noRecogidas();
            entregados += local.packsEntregados();
            numResenas += local.cantidadResenas();
            importeVentas = importeVentas.add(local.importeVentas());
            comisiones = comisiones.add(local.comisiones());
            ingresos = ingresos.add(local.ingresos());
            sumaNotas = sumaNotas.add(local.calificacionPromedio()
                    .multiply(BigDecimal.valueOf(local.cantidadResenas())));
        }
        return new Indicadores(null, "Total empresa", ventas,
                importeVentas, comisiones, ingresos, cancelaciones, noRecogidas,
                numResenas, promedio(sumaNotas, numResenas), entregados, kilos, co2, List.of());
    }

    private Indicadores calcular(Long id, String nombre, List<Reserva> pedidos, List<Resena> opiniones,
            List<ImpactoCO2Evitado> entregas) {
        TotalesVentas totales = new TotalesVentas();
        pedidos.forEach(totales::registrar);
        // Los registros antiguos, con valores vacíos, cuentan como 0.
        BigDecimal kilos = BigDecimal.ZERO;
        BigDecimal co2 = BigDecimal.ZERO;
        for (ImpactoCO2Evitado entrega : entregas) {
            kilos = kilos.add(valorOCero(entrega.getAlimentoRecuperadoKg()));
            co2 = co2.add(valorOCero(entrega.getCo2EvitadoKg()));
        }
        BigDecimal sumaNotas = BigDecimal.ZERO;
        for (Resena opinion : opiniones)
            sumaNotas = sumaNotas.add(BigDecimal.valueOf(opinion.getCalificacion()));
        return new Indicadores(id, nombre, totales.ventas, totales.importeVentas, totales.comisiones,
                totales.ingresos, totales.cancelaciones, totales.noRecogidas, opiniones.size(),
                promedio(sumaNotas, opiniones.size()), totales.entregados, kilos, co2, recientes(opiniones));
    }

    private static BigDecimal valorOCero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    /** Acumula, pedido por pedido, las cifras de venta de un local. */
    private static final class TotalesVentas {
        private int ventas;
        private int cancelaciones;
        private int noRecogidas;
        private int entregados;
        private BigDecimal importeVentas = BigDecimal.ZERO;
        private BigDecimal comisiones = BigDecimal.ZERO;
        private BigDecimal ingresos = BigDecimal.ZERO;

        void registrar(Reserva pedido) {
            Pago pago = pedido.getPago();
            boolean pagado = pago != null && pago.getEstado() == Pago.EstadoPago.CONFIRMADO;
            boolean cancelada = pedido.getEstado() == Reserva.EstadoReserva.CANCELADA;
            if (cancelada) cancelaciones++;
            if (pedido.getEstado() == Reserva.EstadoReserva.EXPIRADA && pagado) noRecogidas++;
            if (!pagado) return;
            if (!cancelada) {
                ventas++;
                importeVentas = importeVentas.add(pedido.getMontoTotal());
            }
            // Los datos anteriores al cálculo de comisiones pueden no tenerla registrada.
            if (pago.getComision() != null) {
                comisiones = comisiones.add(pago.getComision().getImporteComision());
                ingresos = ingresos.add(pago.getComision().getIngresoEstablecimiento());
            }
            if (pedido.getEstado() == Reserva.EstadoReserva.RECOGIDA)
                entregados += pedido.getDetalles().stream().mapToInt(DetalleReserva::getCantidad).sum();
        }
    }

    /** Las cinco reseñas más recientes de un local, con su comentario. */
    private static List<ResenaReciente> recientes(List<Resena> opiniones) {
        return opiniones.stream()
                .sorted(java.util.Comparator.comparing(Resena::getFechaPublicacion).reversed())
                .limit(5)
                .map(o -> new ResenaReciente(o.getCalificacion(), o.getComentario(), o.getFechaPublicacion()))
                .toList();
    }

    private BigDecimal promedio(BigDecimal suma, int cantidad) {
        return cantidad == 0 ? BigDecimal.ZERO
                : suma.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    }

    public record Indicadores(Long establecimientoId, String establecimiento, int ventas,
            BigDecimal importeVentas, BigDecimal comisiones, BigDecimal ingresos,
            int cancelaciones, int noRecogidas, int cantidadResenas,
            BigDecimal calificacionPromedio, int packsEntregados,
            BigDecimal alimentoRecuperadoKg, BigDecimal co2EvitadoKg,
            List<ResenaReciente> resenasRecientes) {}
    public record ResenaReciente(int calificacion, String comentario, java.time.LocalDateTime fecha) {}
    public record Resumen(List<Indicadores> establecimientos, Indicadores total) {}
}
