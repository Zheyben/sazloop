package com.sazloop.ofertas.service;

import com.sazloop.afiliacion.domain.AdministradorPlataforma;
import com.sazloop.afiliacion.domain.SuspensionEstablecimiento;
import com.sazloop.afiliacion.repository.AdministradorPlataformaRepository;
import com.sazloop.afiliacion.repository.SuspensionEstablecimientoRepository;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import com.sazloop.identidad.repository.EstablecimientoRepository;
import com.sazloop.identidad.service.CuentaLocalService;
import com.sazloop.ofertas.EscenarioOfertas;
import com.sazloop.ofertas.EscenarioOfertas.LocalPrueba;
import com.sazloop.ofertas.domain.OfertaPack;
import com.sazloop.ofertas.dto.OfertaResponse;
import com.sazloop.ofertas.repository.OfertaPackRepository;
import com.sazloop.reservas.dto.CompraResponse;
import com.sazloop.shared.Tiempo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.sazloop.ofertas.EscenarioOfertas.unico;
import static org.junit.jupiter.api.Assertions.*;

/** Panel del local: oferta vigente, stock, ganancias por periodo y motivo de suspensión. */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class PanelLocalServiceTests {
    @Autowired ApplicationContext contexto;
    @Autowired PanelLocalService panel;
    @Autowired OfertaPackRepository ofertasRepo;
    @Autowired JdbcTemplate jdbc;
    @Autowired CuentaLocalService cuentas;
    @Autowired AdministradorPlataformaRepository administradores;
    @Autowired SuspensionEstablecimientoRepository suspensiones;
    @Autowired EstablecimientoRepository establecimientos;

    EscenarioOfertas escenario;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioOfertas(contexto);
    }

    @Test
    void localSinOfertasMuestraTodoEnCero() {
        String distrito = unico("PanelVacio");
        LocalPrueba local = escenario.localActivo(distrito, "Calle Vacia");

        PanelLocalService.Resumen resumen = panel.resumen(local.cuenta());
        assertEquals("Local " + distrito, resumen.establecimiento());
        assertNull(resumen.ofertaVigente());
        assertNull(resumen.tokenQr());
        assertEquals(0, resumen.stockDisponible());
        assertEquals(EstadoEstablecimiento.ACTIVO, resumen.estadoLocal());
        assertNull(resumen.motivoSuspension());
        for (PanelLocalService.Periodo periodo : new PanelLocalService.Periodo[] {
                resumen.dia(), resumen.semana(), resumen.mes()}) {
            assertEquals(0, BigDecimal.ZERO.compareTo(periodo.ganancia()));
            assertEquals(0, BigDecimal.ZERO.compareTo(periodo.comision()));
            assertEquals(0, BigDecimal.ZERO.compareTo(periodo.ventaBruta()));
            assertEquals(0, periodo.packsEntregados());
            assertEquals(0, periodo.unidadesNoVendidas());
        }
    }

    @Test
    void cuentaSoloEntregasYSobrantesDelPeriodoYMuestraLaOfertaVigente() {
        LocalPrueba local = escenario.localActivo(unico("Panel"), "Calle Panel");

        // Oferta de hoy, ya vencida: 1 entregada, 1 pagada sin recoger y 1 sin vender.
        OfertaResponse deHoy = escenario.publicar(local, 3);
        escenario.comprarYRecoger(escenario.usuario(), deHoy, local);
        escenario.comprar(escenario.usuario(), deHoy);
        vencer(deHoy.id(), null);

        // Oferta antigua (hace 40 días): su entrega y su sobrante quedan fuera de los tres periodos.
        OfertaResponse antigua = escenario.publicar(local, 2);
        CompraResponse entregaAntigua = escenario.comprarYRecoger(escenario.usuario(), antigua, local);
        LocalDateTime haceCuarentaDias = Tiempo.ahora().minusDays(40);
        vencer(antigua.id(), haceCuarentaDias);
        jdbc.update("update recojos set fecha_recojo = ? where reserva_id = ?",
                haceCuarentaDias, entregaAntigua.reservaId());

        OfertaResponse vigente = escenario.publicar(local, 4);

        PanelLocalService.Resumen resumen = panel.resumen(local.cuenta());
        assertEquals(vigente.id(), resumen.ofertaVigente().id());
        assertEquals(4, resumen.stockDisponible());
        assertEquals(ofertasRepo.findById(vigente.id()).orElseThrow().getTokenQr(), resumen.tokenQr());
        for (PanelLocalService.Periodo periodo : new PanelLocalService.Periodo[] {
                resumen.dia(), resumen.semana(), resumen.mes()}) {
            assertEquals(new BigDecimal("9.00"), periodo.ganancia());
            assertEquals(new BigDecimal("1.00"), periodo.comision());
            assertEquals(new BigDecimal("10.00"), periodo.ventaBruta());
            assertEquals(1, periodo.packsEntregados());
            assertEquals(1, periodo.unidadesNoVendidas());
        }
    }

    @Test
    void localSuspendidoMuestraElMotivoDeLaSuspensionActiva() {
        LocalPrueba local = escenario.localActivo(unico("PanelSuspendido"), "Calle Suspendida");
        AdministradorPlataforma admin = administradores.save(AdministradorPlataforma.builder()
                .nombre("Revisor").emailInstitucional(UUID.randomUUID() + "@example.test")
                .passwordHash("hash-temporal").build());
        suspensiones.save(SuspensionEstablecimiento.builder().administrador(admin)
                .establecimiento(establecimientos.findById(local.localId()).orElseThrow())
                .motivo("Licencia vencida").build());
        escenario.cambiarEstado(local.localId(), EstadoEstablecimiento.SUSPENDIDO);

        PanelLocalService.Resumen resumen = panel.resumen(cuentas.obtener(local.cuenta().getId()));
        assertEquals(EstadoEstablecimiento.SUSPENDIDO, resumen.estadoLocal());
        assertEquals("Licencia vencida", resumen.motivoSuspension());
        assertNull(resumen.ofertaVigente());
    }

    /** Termina el horario de la oferta; si se indica una fecha, la oferta pasa a ser de ese día. */
    private void vencer(Long ofertaId, LocalDateTime fecha) {
        OfertaPack oferta = ofertasRepo.findById(ofertaId).orElseThrow();
        if (fecha != null) {
            oferta.setFechaOferta(fecha.toLocalDate());
            oferta.setInicioRecojo(fecha);
            oferta.setFinRecojo(fecha.plusHours(1));
        } else {
            oferta.setFinRecojo(Tiempo.ahora().minusSeconds(1));
        }
        ofertasRepo.saveAndFlush(oferta);
    }
}
