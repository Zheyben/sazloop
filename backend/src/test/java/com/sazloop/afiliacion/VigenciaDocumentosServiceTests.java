package com.sazloop.afiliacion;

import com.sazloop.afiliacion.domain.AdministradorPlataforma;
import com.sazloop.afiliacion.domain.DocumentoEstablecimiento;
import com.sazloop.afiliacion.domain.DocumentoEstablecimiento.TipoDocumento;
import com.sazloop.afiliacion.dto.SolicitudResponse;
import com.sazloop.afiliacion.service.VigenciaDocumentosService;
import com.sazloop.afiliacion.service.VigenciaDocumentosService.EstadoVigencia;
import com.sazloop.experiencia.domain.Notificacion.TipoNotificacion;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Vigencia de documentos aprobados: estados, suspensión automática y reactivación (RF-09). */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class VigenciaDocumentosServiceTests extends ApoyoAfiliacion {

    @Test
    void estadoDeVigenciaSegunLaFechaDeVencimiento() {
        LocalDate dia = LocalDate.of(2026, 3, 10);
        assertEquals(EstadoVigencia.SIN_VENCIMIENTO, VigenciaDocumentosService.estado(conVencimiento(null), dia));
        assertEquals(EstadoVigencia.VENCIDO, VigenciaDocumentosService.estado(conVencimiento(dia.minusDays(1)), dia));
        assertEquals(EstadoVigencia.POR_VENCER, VigenciaDocumentosService.estado(conVencimiento(dia), dia));
        assertEquals(EstadoVigencia.POR_VENCER, VigenciaDocumentosService.estado(conVencimiento(dia.plusDays(7)), dia));
        assertEquals(EstadoVigencia.VIGENTE, VigenciaDocumentosService.estado(conVencimiento(dia.plusDays(8)), dia));
    }

    @Test
    void laSuspensionDescribeDocumentosSinNumeroYRecortaMotivosLargos() {
        Long propietario = usuario();
        AdministradorPlataforma admin = admin();
        Long local = localPendiente(propietario, "Local muchos vencidos");
        // Cinco certificados con números largos y uno sin número: el motivo supera 500 letras.
        for (int i = 0; i < 5; i++) {
            adjuntar(propietario, local, TipoDocumento.CERTIFICADO_SANITARIO, i + "X".repeat(78), hoy().plusYears(1));
        }
        SolicitudResponse solicitud = adjuntar(propietario, local, TipoDocumento.LICENCIA_FUNCIONAMIENTO, null,
                hoy().plusYears(1));
        solicitudes.enviar(propietario, local);
        aprobarTodo(admin, solicitud, null);
        solicitud.documentos().forEach(d -> cambiarVencimiento(d.id(), hoy().minusDays(3)));

        vigencia.revisarVigencias();
        assertEquals(EstadoEstablecimiento.SUSPENDIDO, estadoLocal(local));
        String motivo = vigencia.suspensionActiva(local).orElseThrow().getMotivo();
        assertEquals(500, motivo.length());
        assertTrue(motivo.startsWith("Documento vencido: Certificado sanitario N.º 0XXX"));
        assertTrue(motivo.endsWith("…"));
        assertEquals("Local suspendido: Local muchos vencidos",
                ultimoAviso(propietario, TipoNotificacion.SUSPENSION).titulo());

        // Una segunda revisión no vuelve a suspender ni duplica el aviso.
        long suspensiones = notificaciones.listar(propietario).stream()
                .filter(a -> a.tipo() == TipoNotificacion.SUSPENSION).count();
        vigencia.revisar(hoy());
        assertEquals(suspensiones, notificaciones.listar(propietario).stream()
                .filter(a -> a.tipo() == TipoNotificacion.SUSPENSION).count());
    }

    @Test
    void unDocumentoSinNumeroSeDescribeSoloConSuTipo() {
        Long propietario = usuario();
        AdministradorPlataforma admin = admin();
        Long local = localPendiente(propietario, "Local sin número");
        SolicitudResponse solicitud = adjuntar(propietario, local, TipoDocumento.INSPECCION_SEGURIDAD, null,
                hoy().plusYears(1));
        solicitudes.enviar(propietario, local);
        aprobarTodo(admin, solicitud, null);
        cambiarVencimiento(solicitud.documentos().getFirst().id(), hoy().minusDays(1));

        vigencia.revisar(hoy());
        assertTrue(vigencia.suspensionActiva(local).orElseThrow().getMotivo()
                .startsWith("Documento vencido: Inspección de seguridad (venció el "));
    }

    @Test
    void unLocalSuspendidoConDocumentoPorVencerRecibeElAvisoUnaSolaVez() {
        Long propietario = usuario();
        AdministradorPlataforma admin = admin();
        Long local = localPendiente(propietario, "Local suspendido con aviso");
        SolicitudResponse solicitud = localAprobado(propietario, local, admin,
                TipoDocumento.CERTIFICADO_SANITARIO, TipoDocumento.LICENCIA_FUNCIONAMIENTO);
        cambiarVencimiento(solicitud.documentos().get(0).id(), hoy().minusDays(1));
        cambiarVencimiento(solicitud.documentos().get(1).id(), hoy().plusDays(2));

        vigencia.revisar(hoy());
        assertEquals(EstadoEstablecimiento.SUSPENDIDO, estadoLocal(local));
        // Ya suspendido, la siguiente revisión avisa del documento por vencer sin duplicar el aviso.
        vigencia.revisar(hoy());
        vigencia.revisar(hoy());
        assertEquals(1, notificaciones.listar(propietario).stream()
                .filter(a -> a.tipo() == TipoNotificacion.DOCUMENTO_POR_VENCER).count());
        assertTrue(ultimoAviso(propietario, TipoNotificacion.DOCUMENTO_POR_VENCER).mensaje()
                .startsWith("Licencia de funcionamiento N.º LICENCIA_FUNCIONAMIENTO-1 vence el "));
    }

    @Test
    void reactivarSoloActuaSobreLocalesSuspendidosSinVencidos() {
        Long propietario = usuario();
        AdministradorPlataforma admin = admin();
        Long activo = localPendiente(propietario, "Local activo sin reactivar");
        localAprobado(propietario, activo, admin, TipoDocumento.OTRO);
        assertFalse(vigencia.reactivar(establecimientosRepo.findById(activo).orElseThrow()));
        assertEquals(EstadoEstablecimiento.ACTIVO, estadoLocal(activo));

        Long suspendido = localPendiente(propietario, "Local con vencido pendiente");
        SolicitudResponse solicitud = localAprobado(propietario, suspendido, admin, TipoDocumento.OTRO);
        cambiarVencimiento(solicitud.documentos().getFirst().id(), hoy().minusDays(1));
        vigencia.revisar(hoy());
        assertFalse(vigencia.reactivar(establecimientosRepo.findById(suspendido).orElseThrow()));
        assertEquals(EstadoEstablecimiento.SUSPENDIDO, estadoLocal(suspendido));
        assertTrue(vigencia.suspensionActiva(suspendido).isPresent());
    }

    private static DocumentoEstablecimiento conVencimiento(LocalDate vencimiento) {
        return DocumentoEstablecimiento.builder().tipoDocumento(TipoDocumento.OTRO)
                .fechaVencimiento(vencimiento).build();
    }
}
