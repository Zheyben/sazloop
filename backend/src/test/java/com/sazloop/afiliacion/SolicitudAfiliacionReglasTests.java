package com.sazloop.afiliacion;

import com.sazloop.afiliacion.domain.AdministradorPlataforma;
import com.sazloop.afiliacion.domain.DocumentoEstablecimiento.TipoDocumento;
import com.sazloop.afiliacion.domain.SolicitudAfiliacion.EstadoSolicitud;
import com.sazloop.afiliacion.dto.DocumentacionResponse;
import com.sazloop.afiliacion.dto.SolicitudResponse;
import com.sazloop.afiliacion.service.SolicitudAfiliacionService.ArchivoPrivado;
import com.sazloop.afiliacion.service.VigenciaDocumentosService.EstadoVigencia;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas del propietario al preparar, enviar y renovar la documentación de su local (RF-07 y RF-09). */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class SolicitudAfiliacionReglasTests extends ApoyoAfiliacion {

    private static ResponseStatusException error(HttpStatus esperado, Executable accion) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado, ex.getStatusCode());
        return ex;
    }

    @Test
    void unLocalInexistenteOAjenoNoSeEncuentra() {
        Long propietario = usuario();
        Long otro = usuario();
        Long local = localPendiente(propietario, "Local privado");

        error(HttpStatus.NOT_FOUND, () -> solicitudes.consultar(propietario, -1L));
        error(HttpStatus.NOT_FOUND, () -> solicitudes.documentacion(otro, local));
        error(HttpStatus.NOT_FOUND, () -> solicitudes.enviar(otro, local));
        error(HttpStatus.NOT_FOUND, () -> solicitudes.adjuntar(otro, local, TipoDocumento.OTRO,
                null, null, null, pdf()));
        assertTrue(solicitudes.consultar(propietario, local).isEmpty());
    }

    @Test
    void noSeEnviaSinDocumentosNiDosVeces() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local sin adjuntos");

        ResponseStatusException vacio = error(HttpStatus.CONFLICT, () -> solicitudes.enviar(propietario, local));
        assertEquals("Adjunta documentos primero", vacio.getReason());

        adjuntar(propietario, local, TipoDocumento.OTRO, "  ", null);
        SolicitudResponse enviada = solicitudes.enviar(propietario, local);
        assertEquals(EstadoSolicitud.EN_REVISION, enviada.estado());
        assertNull(enviada.documentos().getFirst().numero());
        assertFalse(enviada.renovacion());

        ResponseStatusException repetida = error(HttpStatus.CONFLICT, () -> solicitudes.enviar(propietario, local));
        assertEquals("La solicitud no está lista para enviarse", repetida.getReason());
        ResponseStatusException adjuntoTarde = error(HttpStatus.CONFLICT, () -> adjuntar(propietario, local,
                TipoDocumento.OTRO, null, null));
        assertEquals("La solicitud ya fue enviada; espera la revisión del administrador", adjuntoTarde.getReason());
    }

    @Test
    void lasFechasDelDocumentoDebenSerCoherentesYVigentes() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local fechas");

        ResponseStatusException orden = error(HttpStatus.BAD_REQUEST, () -> solicitudes.adjuntar(propietario, local,
                TipoDocumento.CERTIFICADO_SANITARIO, "CS", hoy(), hoy().minusDays(1), pdf()));
        assertEquals("La fecha de vencimiento precede a la emisión", orden.getReason());
        ResponseStatusException vencido = error(HttpStatus.BAD_REQUEST, () -> solicitudes.adjuntar(propietario, local,
                TipoDocumento.CERTIFICADO_SANITARIO, "CS", null, hoy().minusDays(1), pdf()));
        assertEquals("El documento ya está vencido", vencido.getReason());

        SolicitudResponse valido = solicitudes.adjuntar(propietario, local, TipoDocumento.CERTIFICADO_SANITARIO,
                " CS-10 ", hoy().minusYears(1), hoy(), pdf());
        assertEquals("CS-10", valido.documentos().getFirst().numero());
        assertEquals(hoy(), valido.documentos().getFirst().fechaVencimiento());
    }

    @Test
    void unaFechaDeEmisionSinVencimientoSeAceptaYUnaSolicitudVaciaNoSeEnvia() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local solo emisión");
        // Solicitud pendiente creada sin documentos: no está lista para enviarse.
        solicitudesRepo.saveAndFlush(com.sazloop.afiliacion.domain.SolicitudAfiliacion.builder()
                .establecimiento(establecimientosRepo.findById(local).orElseThrow()).build());
        ResponseStatusException vacia = error(HttpStatus.CONFLICT, () -> solicitudes.enviar(propietario, local));
        assertEquals("La solicitud no está lista para enviarse", vacia.getReason());

        SolicitudResponse conEmision = solicitudes.adjuntar(propietario, local, TipoDocumento.OTRO, null,
                hoy().minusMonths(2), null, pdf());
        assertEquals(hoy().minusMonths(2), conEmision.documentos().getFirst().fechaEmision());
        assertNull(conEmision.documentos().getFirst().fechaVencimiento());
        assertEquals(EstadoSolicitud.EN_REVISION, solicitudes.enviar(propietario, local).estado());
    }

    @Test
    void unLocalInactivoNoAdmiteSolicitudes() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local inactivo");
        cambiarEstadoLocal(local, EstadoEstablecimiento.INACTIVO);

        ResponseStatusException ex = error(HttpStatus.CONFLICT, () -> adjuntar(propietario, local,
                TipoDocumento.OTRO, null, null));
        assertEquals("El local no admite una solicitud nueva", ex.getReason());
    }

    @Test
    void siFallaElGuardadoDelDocumentoSeBorraElArchivo() throws IOException {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local número largo");
        Path carpeta = Path.of("target/test-documentos");
        Files.createDirectories(carpeta);
        long antes = archivos(carpeta);

        // El número supera las 80 letras de la columna: la base de datos rechaza el registro.
        assertThrows(RuntimeException.class, () -> adjuntar(propietario, local, TipoDocumento.OTRO,
                "N".repeat(120), null));
        assertEquals(antes, archivos(carpeta));
        assertTrue(solicitudes.consultar(propietario, local).isEmpty());
    }

    @Test
    void elPropietarioSoloDescargaDocumentosDeEseLocal() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local A");
        Long otroLocal = localPendiente(propietario, "Local B");
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1, 2};
        SolicitudResponse conPng = solicitudes.adjuntar(propietario, local, TipoDocumento.OTRO, null, null, null,
                new MockMultipartFile("archivo", "foto.png", "image/png", png));
        Long documento = conPng.documentos().getFirst().id();

        ArchivoPrivado archivo = solicitudes.descargar(propietario, local, documento);
        assertEquals(new ArchivoPrivado(png, "png"), archivo);
        error(HttpStatus.NOT_FOUND, () -> solicitudes.descargar(propietario, otroLocal, documento));
        error(HttpStatus.NOT_FOUND, () -> solicitudes.descargar(propietario, local, -1L));
    }

    @Test
    void localSuspendidoDebeRenovarTodosLosDocumentosVencidos() {
        Long propietario = usuario();
        AdministradorPlataforma admin = admin();
        Long local = localPendiente(propietario, "Local renovación completa");
        SolicitudResponse original = localAprobado(propietario, local, admin,
                TipoDocumento.CERTIFICADO_SANITARIO, TipoDocumento.LICENCIA_FUNCIONAMIENTO,
                TipoDocumento.INSPECCION_SEGURIDAD);
        cambiarVencimiento(original.documentos().get(0).id(), hoy().minusDays(1));
        cambiarVencimiento(original.documentos().get(1).id(), hoy().minusDays(2));
        cambiarVencimiento(original.documentos().get(2).id(), hoy().plusDays(5));
        vigencia.revisar(hoy());

        DocumentacionResponse estado = solicitudes.documentacion(propietario, local);
        assertEquals(EstadoEstablecimiento.SUSPENDIDO, estado.estado());
        assertTrue(estado.motivoSuspension().contains("Certificado sanitario N.º CERTIFICADO_SANITARIO-1"));
        assertNotNull(estado.suspendidoDesde());
        assertEquals(List.of(TipoDocumento.CERTIFICADO_SANITARIO, TipoDocumento.LICENCIA_FUNCIONAMIENTO),
                estado.tiposVencidos());
        assertEquals(List.of(TipoDocumento.CERTIFICADO_SANITARIO, TipoDocumento.LICENCIA_FUNCIONAMIENTO,
                TipoDocumento.INSPECCION_SEGURIDAD), estado.tiposRenovables());
        assertEquals(EstadoVigencia.POR_VENCER, estado.documentos().get(2).vigencia());
        assertEquals(EstadoSolicitud.APROBADA, estado.solicitud().estado());
        assertEquals("Documentación revisada y conforme", estado.solicitud().motivoDecision());

        // Un tipo que no está vencido ni por vencer no se puede renovar.
        Long otro = localPendiente(propietario, "Local sin renovables");
        localAprobado(propietario, otro, admin, TipoDocumento.OTRO);
        ResponseStatusException noRenovable = error(HttpStatus.CONFLICT, () -> adjuntar(propietario, otro,
                TipoDocumento.OTRO, null, hoy().plusYears(2)));
        assertEquals("Solo puedes renovar documentos vencidos o que vencen en los próximos 7 días",
                noRenovable.getReason());

        // Con solo uno de los dos vencidos renovado, el envío se bloquea indicando el faltante.
        SolicitudResponse renovacion = adjuntar(propietario, local, TipoDocumento.CERTIFICADO_SANITARIO, "CS-2",
                hoy().plusYears(1));
        assertTrue(renovacion.renovacion());
        ResponseStatusException faltante = error(HttpStatus.CONFLICT, () -> solicitudes.enviar(propietario, local));
        assertEquals("Adjunta la renovación de: Licencia de funcionamiento", faltante.getReason());

        adjuntar(propietario, local, TipoDocumento.LICENCIA_FUNCIONAMIENTO, "LF-2", hoy().plusYears(1));
        assertEquals(EstadoSolicitud.EN_REVISION, solicitudes.enviar(propietario, local).estado());
        assertEquals(EstadoSolicitud.EN_REVISION, solicitudes.consultar(propietario, local).orElseThrow().estado());
    }

    @Test
    void archivoPrivadoSeComparaPorContenido() {
        ArchivoPrivado a = new ArchivoPrivado(new byte[] {1, 2, 3}, "pdf");
        ArchivoPrivado b = new ArchivoPrivado(new byte[] {1, 2, 3}, "pdf");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, List.of(a).get(0));
        assertNotEquals(a, new ArchivoPrivado(new byte[] {1, 2, 4}, "pdf"));
        assertNotEquals(a, new ArchivoPrivado(new byte[] {1, 2, 3}, "png"));
        Object otroObjeto = "otro objeto";
        assertNotEquals(a, otroObjeto);
        assertEquals("ArchivoPrivado[contenido=3 bytes, extension=pdf]", a.toString());
        ArchivoPrivado vacio = new ArchivoPrivado(null, null);
        assertEquals("ArchivoPrivado[contenido=0 bytes, extension=null]", vacio.toString());
        assertEquals(vacio, new ArchivoPrivado(null, null));
        assertEquals(vacio.hashCode(), new ArchivoPrivado(null, null).hashCode());
    }

    private static long archivos(Path carpeta) throws IOException {
        try (Stream<Path> lista = Files.list(carpeta)) {
            return lista.count();
        }
    }
}
