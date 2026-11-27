package com.sazloop.afiliacion;

import com.sazloop.afiliacion.domain.AdministradorPlataforma;
import com.sazloop.afiliacion.domain.DocumentoEstablecimiento.TipoDocumento;
import com.sazloop.afiliacion.domain.EvaluacionAfiliacion.DecisionAfiliacion;
import com.sazloop.afiliacion.domain.RevisionDocumento.ResultadoRevision;
import com.sazloop.afiliacion.domain.SolicitudAfiliacion;
import com.sazloop.afiliacion.domain.SolicitudAfiliacion.EstadoSolicitud;
import com.sazloop.afiliacion.domain.SuspensionEstablecimiento;
import com.sazloop.afiliacion.dto.DecisionRequest;
import com.sazloop.afiliacion.dto.EstablecimientoAdminResponse;
import com.sazloop.afiliacion.dto.ResolucionRequest;
import com.sazloop.afiliacion.dto.RevisionRequest;
import com.sazloop.afiliacion.dto.SolicitudResponse;
import com.sazloop.afiliacion.dto.SolicitudRevisionResponse;
import com.sazloop.afiliacion.repository.EvaluacionAfiliacionRepository;
import com.sazloop.afiliacion.repository.SuspensionEstablecimientoRepository;
import com.sazloop.afiliacion.service.SolicitudAfiliacionService.ArchivoPrivado;
import com.sazloop.experiencia.domain.Notificacion.TipoNotificacion;
import com.sazloop.experiencia.service.NotificacionService;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Revisión de documentos y resolución de solicitudes y renovaciones (RF-07, RF-08 y RF-09). */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class RevisionAfiliacionServiceTests extends ApoyoAfiliacion {
    @Autowired SuspensionEstablecimientoRepository suspensiones;
    @Autowired EvaluacionAfiliacionRepository evaluaciones;

    private static ResponseStatusException error(HttpStatus esperado, Executable accion) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado, ex.getStatusCode());
        return ex;
    }

    @Test
    void solicitudesYDocumentosInexistentesRespondenNoEncontrado() {
        AdministradorPlataforma admin = admin();
        error(HttpStatus.NOT_FOUND, () -> revision.consultar(-1L));
        error(HttpStatus.NOT_FOUND, () -> revision.descargar(-1L));
        error(HttpStatus.NOT_FOUND, () -> revision.revisar(admin, -1L,
                new RevisionRequest(ResultadoRevision.APROBADO, null)));
        error(HttpStatus.NOT_FOUND, () -> revision.decidir(admin, -1L,
                new DecisionRequest(DecisionAfiliacion.APROBADA, "Conforme")));
        error(HttpStatus.NOT_FOUND, () -> revision.resolver(admin, -1L, new ResolucionRequest(
                List.of(new ResolucionRequest.Documento(-1L, ResultadoRevision.APROBADO, null)), null)));
    }

    @Test
    void elAdministradorDescargaElArchivoPrivadoConSuExtension() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local descarga admin");
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local, TipoDocumento.LICENCIA_FUNCIONAMIENTO);

        ArchivoPrivado archivo = revision.descargar(solicitud.documentos().getFirst().id());
        assertArrayEquals(PDF, archivo.contenido());
        assertEquals("pdf", archivo.extension());
    }

    @Test
    void noSeRevisaUnDocumentoDeUnaSolicitudQueAunNoSeEnvia() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local sin enviar");
        SolicitudResponse pendiente = adjuntar(propietario, local, TipoDocumento.OTRO, null, null);

        ResponseStatusException ex = error(HttpStatus.CONFLICT, () -> revision.revisar(admin(),
                pendiente.documentos().getFirst().id(), new RevisionRequest(ResultadoRevision.APROBADO, null)));
        assertEquals("La solicitud no está en revisión", ex.getReason());
        error(HttpStatus.CONFLICT, () -> revision.decidir(admin(), pendiente.id(),
                new DecisionRequest(DecisionAfiliacion.APROBADA, "Conforme")));
    }

    @Test
    void unDocumentoNoSeRevisaDosVeces() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local doble revisión");
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local, TipoDocumento.CERTIFICADO_SANITARIO);
        Long documento = solicitud.documentos().getFirst().id();
        AdministradorPlataforma admin = admin();

        revision.revisar(admin, documento, new RevisionRequest(ResultadoRevision.APROBADO, "Legible"));
        ResponseStatusException ex = error(HttpStatus.CONFLICT, () -> revision.revisar(admin, documento,
                new RevisionRequest(ResultadoRevision.RECHAZADO, "Cambio de opinión")));
        assertEquals("El documento ya fue revisado", ex.getReason());
        assertEquals(ResultadoRevision.APROBADO, revision.consultar(solicitud.id()).documentos().getFirst().resultado());
    }

    @Test
    void rechazarExigeObservaciones() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local sin observaciones");
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local, TipoDocumento.CERTIFICADO_SANITARIO);
        Long documento = solicitud.documentos().getFirst().id();
        AdministradorPlataforma admin = admin();

        ResponseStatusException sinTexto = error(HttpStatus.BAD_REQUEST, () -> revision.revisar(admin, documento,
                new RevisionRequest(ResultadoRevision.RECHAZADO, null)));
        assertEquals("Indica las observaciones del rechazo", sinTexto.getReason());
        error(HttpStatus.BAD_REQUEST, () -> revision.revisar(admin, documento,
                new RevisionRequest(ResultadoRevision.RECHAZADO, "   ")));
        assertNull(revision.consultar(solicitud.id()).documentos().getFirst().resultado());

        SolicitudRevisionResponse rechazado = revision.revisar(admin, documento,
                new RevisionRequest(ResultadoRevision.RECHAZADO, "Foto borrosa"));
        assertEquals(ResultadoRevision.RECHAZADO, rechazado.documentos().getFirst().resultado());
        assertEquals("Foto borrosa", rechazado.documentos().getFirst().observaciones());
    }

    @Test
    void elRevisorCorrigeTipoNumeroYFechasDelDocumento() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local corrección");
        adjuntar(propietario, local, TipoDocumento.OTRO, "ORIGINAL", hoy().plusYears(1));
        SolicitudResponse solicitud = adjuntar(propietario, local, TipoDocumento.OTRO, "BORRAR", hoy().plusYears(1));
        solicitudes.enviar(propietario, local);
        Long primero = solicitud.documentos().get(0).id();
        Long segundo = solicitud.documentos().get(1).id();
        AdministradorPlataforma admin = admin();

        // Un vencimiento anterior a la emisión no se acepta y no se guarda nada.
        ResponseStatusException fechas = error(HttpStatus.BAD_REQUEST, () -> revision.revisar(admin, primero,
                new RevisionRequest(ResultadoRevision.APROBADO, null, null, null,
                        hoy().plusDays(10), hoy().plusDays(5))));
        assertEquals("Vencimiento anterior a emisión", fechas.getReason());

        SolicitudRevisionResponse corregida = revision.revisar(admin, primero, new RevisionRequest(
                ResultadoRevision.APROBADO, "Datos corregidos", TipoDocumento.LICENCIA_FUNCIONAMIENTO,
                "  LF-2026  ", hoy().minusYears(1), hoy().plusYears(2)));
        SolicitudRevisionResponse.DocumentoEnRevision doc = corregida.documentos().get(0);
        assertEquals("LICENCIA_FUNCIONAMIENTO", doc.tipo());
        assertEquals("LF-2026", doc.numero());
        assertEquals(hoy().minusYears(1), doc.emision());
        assertEquals(hoy().plusYears(2), doc.vencimiento());

        // Un número en blanco borra el número transcrito; un rechazo de un vencido sí se admite.
        SolicitudRevisionResponse sinNumero = revision.revisar(admin, segundo, new RevisionRequest(
                ResultadoRevision.RECHAZADO, "Vencido", null, " ", null, hoy().minusDays(2)));
        assertNull(sinNumero.documentos().get(1).numero());
        assertEquals(hoy().minusDays(2), sinNumero.documentos().get(1).vencimiento());
    }

    @Test
    void corregirSoloLaEmisionDeUnDocumentoSinVencimientoSeAcepta() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local solo emisión admin");
        SolicitudResponse solicitud = adjuntar(propietario, local, TipoDocumento.OTRO, null, null);
        solicitudes.enviar(propietario, local);

        SolicitudRevisionResponse revisada = revision.revisar(admin(), solicitud.documentos().getFirst().id(),
                new RevisionRequest(ResultadoRevision.APROBADO, null, null, null, hoy().minusDays(10), null));
        assertEquals(hoy().minusDays(10), revisada.documentos().getFirst().emision());
        assertNull(revisada.documentos().getFirst().vencimiento());
        assertEquals(ResultadoRevision.APROBADO, revisada.documentos().getFirst().resultado());
    }

    @Test
    void unMotivoHechoSoloDePuntosNoDuplicaLaPuntuacionDelAviso() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local motivo de puntos");
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local, TipoDocumento.OTRO);
        AdministradorPlataforma admin = admin();
        revision.revisar(admin, solicitud.documentos().getFirst().id(),
                new RevisionRequest(ResultadoRevision.RECHAZADO, "Ilegible"));

        revision.decidir(admin, solicitud.id(), new DecisionRequest(DecisionAfiliacion.RECHAZADA, " . . "));
        assertEquals(". .", revision.consultar(solicitud.id()).motivoDecision());
        assertEquals("Motivo: . Puedes adjuntar nuevos documentos y enviar otra solicitud.",
                ultimoAviso(propietario, TipoNotificacion.RESULTADO_AFILIACION).mensaje());
    }

    @Test
    void resolverExigeElResultadoDeCadaDocumentoSinRevisar() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local resolución incompleta");
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local,
                TipoDocumento.CERTIFICADO_SANITARIO, TipoDocumento.LICENCIA_FUNCIONAMIENTO);
        Long primero = solicitud.documentos().get(0).id();
        AdministradorPlataforma admin = admin();

        ResponseStatusException ex = error(HttpStatus.BAD_REQUEST, () -> revision.resolver(admin, solicitud.id(),
                new ResolucionRequest(List.of(new ResolucionRequest.Documento(primero,
                        ResultadoRevision.APROBADO, null)), null)));
        assertEquals("Indica el resultado de cada documento: aprobar o rechazar", ex.getReason());
        assertEquals(EstadoSolicitud.EN_REVISION, revision.consultar(solicitud.id()).estado());
    }

    @Test
    void resolverReutilizaLaRevisionPreviaYUsaElComentarioDelAdministrador() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local comentario");
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local,
                TipoDocumento.CERTIFICADO_SANITARIO, TipoDocumento.LICENCIA_FUNCIONAMIENTO);
        Long primero = solicitud.documentos().get(0).id();
        Long segundo = solicitud.documentos().get(1).id();
        AdministradorPlataforma admin = admin();
        revision.revisar(admin, primero, new RevisionRequest(ResultadoRevision.RECHAZADO, "Sin sello"));

        // El resultado enviado para el primero se ignora: ya tenía una revisión.
        SolicitudRevisionResponse resuelta = revision.resolver(admin, solicitud.id(), new ResolucionRequest(List.of(
                new ResolucionRequest.Documento(primero, ResultadoRevision.APROBADO, null),
                new ResolucionRequest.Documento(segundo, ResultadoRevision.APROBADO, null)),
                "  Falta el sello municipal  "));
        assertEquals(EstadoSolicitud.RECHAZADA, resuelta.estado());
        assertEquals("Falta el sello municipal", resuelta.motivoDecision());
        assertEquals(ResultadoRevision.RECHAZADO, resuelta.documentos().get(0).resultado());
        assertEquals(ResultadoRevision.APROBADO, resuelta.documentos().get(1).resultado());
        assertEquals(EstadoEstablecimiento.PENDIENTE_APROBACION, estadoLocal(local));
    }

    @Test
    void rechazoSinComentarioArmaElMotivoYElAvisoSinPuntosRepetidos() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local motivo armado");
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local,
                TipoDocumento.CERTIFICADO_SANITARIO, TipoDocumento.LICENCIA_FUNCIONAMIENTO);
        AdministradorPlataforma admin = admin();

        SolicitudRevisionResponse resuelta = revision.resolver(admin, solicitud.id(), new ResolucionRequest(List.of(
                new ResolucionRequest.Documento(solicitud.documentos().get(0).id(), ResultadoRevision.APROBADO, null),
                new ResolucionRequest.Documento(solicitud.documentos().get(1).id(), ResultadoRevision.RECHAZADO,
                        " Ilegible... ")), "   "));
        assertEquals(EstadoSolicitud.RECHAZADA, resuelta.estado());
        assertEquals("Licencia de funcionamiento: Ilegible...", resuelta.motivoDecision());

        NotificacionService.Aviso aviso = ultimoAviso(propietario, TipoNotificacion.RESULTADO_AFILIACION);
        assertEquals("Solicitud rechazada: Local motivo armado", aviso.titulo());
        assertEquals("Motivo: Licencia de funcionamiento: Ilegible. Puedes adjuntar nuevos documentos "
                + "y enviar otra solicitud.", aviso.mensaje());
    }

    @Test
    void unComentarioMuyLargoSeRecortaA500Caracteres() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local comentario largo");
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local, TipoDocumento.CERTIFICADO_SANITARIO);

        aprobarTodo(admin(), solicitud, "a".repeat(600));
        String motivo = revision.consultar(solicitud.id()).motivoDecision();
        assertEquals(500, motivo.length());
        assertTrue(motivo.endsWith("…"));
        assertEquals(EstadoEstablecimiento.ACTIVO, estadoLocal(local));
        assertTrue(ultimoAviso(propietario, TipoNotificacion.RESULTADO_AFILIACION).titulo()
                .startsWith("Solicitud aprobada"));
    }

    @Test
    void unaSolicitudSinDocumentosNoPuedeResolverse() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local sin documentos");
        SolicitudAfiliacion vacia = solicitudesRepo.save(SolicitudAfiliacion.builder()
                .establecimiento(establecimientosRepo.findById(local).orElseThrow())
                .estado(EstadoSolicitud.EN_REVISION).build());
        AdministradorPlataforma admin = admin();

        ResponseStatusException ex = error(HttpStatus.CONFLICT, () -> revision.resolver(admin, vacia.getId(),
                new ResolucionRequest(List.of(new ResolucionRequest.Documento(-1L, ResultadoRevision.APROBADO, null)),
                        null)));
        assertEquals("Revisa todos los documentos antes de decidir", ex.getReason());
        assertTrue(revision.consultar(vacia.getId()).documentos().isEmpty());
    }

    @Test
    void noSeDecideSinRevisarTodosLosDocumentos() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local revisión parcial");
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local,
                TipoDocumento.CERTIFICADO_SANITARIO, TipoDocumento.LICENCIA_FUNCIONAMIENTO);
        AdministradorPlataforma admin = admin();
        revision.revisar(admin, solicitud.documentos().get(0).id(),
                new RevisionRequest(ResultadoRevision.APROBADO, null));

        ResponseStatusException ex = error(HttpStatus.CONFLICT, () -> revision.decidir(admin, solicitud.id(),
                new DecisionRequest(DecisionAfiliacion.RECHAZADA, "Incompleta")));
        assertEquals("Revisa todos los documentos antes de decidir", ex.getReason());
    }

    @Test
    void noSeApruebaConDocumentosRechazadosOVencidos() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local no aprobable");
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local,
                TipoDocumento.CERTIFICADO_SANITARIO, TipoDocumento.LICENCIA_FUNCIONAMIENTO);
        Long primero = solicitud.documentos().get(0).id();
        Long segundo = solicitud.documentos().get(1).id();
        AdministradorPlataforma admin = admin();
        revision.revisar(admin, primero, new RevisionRequest(ResultadoRevision.APROBADO, null));
        revision.revisar(admin, segundo, new RevisionRequest(ResultadoRevision.RECHAZADO, "Incompleto"));

        ResponseStatusException rechazado = error(HttpStatus.CONFLICT, () -> revision.decidir(admin, solicitud.id(),
                new DecisionRequest(DecisionAfiliacion.APROBADA, "Conforme")));
        assertEquals("No se puede aprobar con documentos rechazados o vencidos", rechazado.getReason());

        // Otro local con todo aprobado, pero uno de sus documentos vence antes de la decisión.
        Long otroLocal = localPendiente(propietario, "Local vencido tras revisar");
        SolicitudResponse otra = solicitudEnRevision(propietario, otroLocal, TipoDocumento.CERTIFICADO_SANITARIO);
        Long doc = otra.documentos().getFirst().id();
        revision.revisar(admin, doc, new RevisionRequest(ResultadoRevision.APROBADO, null));
        cambiarVencimiento(doc, hoy().minusDays(1));
        error(HttpStatus.CONFLICT, () -> revision.decidir(admin, otra.id(),
                new DecisionRequest(DecisionAfiliacion.APROBADA, "Conforme")));
        assertEquals(EstadoSolicitud.EN_REVISION, revision.consultar(otra.id()).estado());
        assertEquals(EstadoEstablecimiento.PENDIENTE_APROBACION, estadoLocal(otroLocal));
    }

    @Test
    void unaSolicitudResueltaNoSeVuelveADecidir() {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local ya resuelto");
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local, TipoDocumento.CERTIFICADO_SANITARIO);
        AdministradorPlataforma admin = admin();
        aprobarTodo(admin, solicitud, null);
        assertEquals("Documentación revisada y conforme", revision.consultar(solicitud.id()).motivoDecision());
        DecisionRequest otraVez = new DecisionRequest(DecisionAfiliacion.RECHAZADA, "Otra decisión");

        ResponseStatusException cerrada = error(HttpStatus.CONFLICT, () -> revision.decidir(admin, solicitud.id(), otraVez));
        assertEquals("La solicitud no está en revisión", cerrada.getReason());

        // Aunque el estado se reabra a mano, la evaluación existente impide una segunda decisión.
        SolicitudAfiliacion entidad = solicitudesRepo.findById(solicitud.id()).orElseThrow();
        entidad.setEstado(EstadoSolicitud.EN_REVISION);
        solicitudesRepo.saveAndFlush(entidad);
        ResponseStatusException repetida = error(HttpStatus.CONFLICT, () -> revision.decidir(admin, solicitud.id(), otraVez));
        assertEquals("La solicitud ya fue resuelta", repetida.getReason());
        assertTrue(evaluaciones.existsBySolicitudAfiliacion_Id(solicitud.id()));
    }

    @Test
    void listadosDelAdministradorMuestranEstadoSuspensionYSolicitudEnRevision() {
        Long propietario = usuario();
        AdministradorPlataforma admin = admin();
        Long enRevision = localPendiente(propietario, "Local en revisión");
        SolicitudResponse pendiente = solicitudEnRevision(propietario, enRevision, TipoDocumento.OTRO);

        Long automatico = localPendiente(propietario, "Local suspendido automático");
        SolicitudResponse aprobada = localAprobado(propietario, automatico, admin, TipoDocumento.CERTIFICADO_SANITARIO);
        cambiarVencimiento(aprobada.documentos().getFirst().id(), hoy().minusDays(1));
        vigencia.revisar(hoy());

        Long manual = localPendiente(propietario, "Local suspendido manual");
        localAprobado(propietario, manual, admin, TipoDocumento.LICENCIA_FUNCIONAMIENTO);
        cambiarEstadoLocal(manual, EstadoEstablecimiento.SUSPENDIDO);
        suspensiones.saveAndFlush(SuspensionEstablecimiento.builder()
                .establecimiento(establecimientosRepo.findById(manual).orElseThrow())
                .administrador(admin).motivo("Denuncia sanitaria").build());

        assertTrue(revision.pendientes().stream().anyMatch(s -> s.id().equals(pendiente.id())));
        List<EstablecimientoAdminResponse> lista = revision.establecimientos();
        EstablecimientoAdminResponse a = buscar(lista, enRevision);
        assertTrue(a.solicitudEnRevision());
        assertNull(a.motivoSuspension());
        assertNull(a.suspendidoDesde());
        assertFalse(a.suspensionAutomatica());
        assertEquals(EstadoEstablecimiento.PENDIENTE_APROBACION, a.estado());

        EstablecimientoAdminResponse b = buscar(lista, automatico);
        assertEquals(EstadoEstablecimiento.SUSPENDIDO, b.estado());
        assertTrue(b.motivoSuspension().startsWith("Documento vencido: Certificado sanitario"));
        assertNotNull(b.suspendidoDesde());
        assertTrue(b.suspensionAutomatica());
        assertFalse(b.solicitudEnRevision());

        EstablecimientoAdminResponse c = buscar(lista, manual);
        assertEquals("Denuncia sanitaria", c.motivoSuspension());
        assertFalse(c.suspensionAutomatica());
    }

    @Test
    void renovacionDeUnLocalActivoAprobadaORechazadaAvisaAlPropietario() {
        Long propietario = usuario();
        AdministradorPlataforma admin = admin();
        Long local = localPendiente(propietario, "Local renovación activa");
        SolicitudResponse original = localAprobado(propietario, local, admin, TipoDocumento.CERTIFICADO_SANITARIO);
        cambiarVencimiento(original.documentos().getFirst().id(), hoy().plusDays(3));

        // Primera renovación: se rechaza y el local sigue activo.
        SolicitudResponse renovacion = adjuntar(propietario, local, TipoDocumento.CERTIFICADO_SANITARIO, "CS-R1",
                hoy().plusYears(1));
        assertTrue(renovacion.renovacion());
        solicitudes.enviar(propietario, local);
        assertTrue(revision.consultar(renovacion.id()).renovacion());
        revision.resolver(admin, renovacion.id(), new ResolucionRequest(List.of(new ResolucionRequest.Documento(
                renovacion.documentos().getFirst().id(), ResultadoRevision.RECHAZADO, "Fecha ilegible.")), null));
        NotificacionService.Aviso rechazo = ultimoAviso(propietario, TipoNotificacion.RESULTADO_AFILIACION);
        assertEquals("Renovación rechazada: Local renovación activa", rechazo.titulo());
        assertEquals("Motivo: Certificado sanitario: Fecha ilegible. Presenta nuevamente los documentos antes "
                + "de que venzan.", rechazo.mensaje());
        assertEquals(EstadoEstablecimiento.ACTIVO, estadoLocal(local));

        // Segunda renovación: se aprueba y reemplaza al documento anterior.
        SolicitudResponse segunda = adjuntar(propietario, local, TipoDocumento.CERTIFICADO_SANITARIO, "CS-R2",
                hoy().plusYears(1));
        solicitudes.enviar(propietario, local);
        aprobarTodo(admin, segunda, "Renovación conforme");
        NotificacionService.Aviso aprobado = ultimoAviso(propietario, TipoNotificacion.RESULTADO_AFILIACION);
        assertEquals("Renovación aprobada: Local renovación activa", aprobado.titulo());
        assertEquals("Los documentos renovados reemplazan a los anteriores. Motivo: Renovación conforme",
                aprobado.mensaje());
        assertEquals(EstadoEstablecimiento.ACTIVO, estadoLocal(local));
        assertEquals("CS-R2", solicitudes.documentacion(propietario, local).documentos().getFirst().numero());
    }

    @Test
    void renovacionDeUnLocalSuspendidoSoloLoReactivaSiNoQuedanVencidos() {
        Long propietario = usuario();
        AdministradorPlataforma admin = admin();
        Long local = localPendiente(propietario, "Local renovación suspendida");
        SolicitudResponse original = localAprobado(propietario, local, admin,
                TipoDocumento.CERTIFICADO_SANITARIO, TipoDocumento.LICENCIA_FUNCIONAMIENTO);
        Long sanitario = original.documentos().get(0).id();
        Long licencia = original.documentos().get(1).id();

        // El certificado está por vencer y se presenta su renovación mientras el local está activo.
        cambiarVencimiento(sanitario, hoy().plusDays(3));
        SolicitudResponse renovacion = adjuntar(propietario, local, TipoDocumento.CERTIFICADO_SANITARIO, "CS-N",
                hoy().plusYears(1));
        solicitudes.enviar(propietario, local);
        // Mientras espera la revisión vence la licencia y el local se suspende.
        cambiarVencimiento(licencia, hoy().minusDays(1));
        vigencia.revisar(hoy());
        assertEquals(EstadoEstablecimiento.SUSPENDIDO, estadoLocal(local));

        aprobarTodo(admin, renovacion, null);
        NotificacionService.Aviso aprobado = ultimoAviso(propietario, TipoNotificacion.RESULTADO_AFILIACION);
        assertEquals("Renovación aprobada: Local renovación suspendida", aprobado.titulo());
        assertTrue(aprobado.mensaje().startsWith("El local sigue suspendido porque aún tiene documentos vencidos."));
        assertEquals(EstadoEstablecimiento.SUSPENDIDO, estadoLocal(local));
        assertTrue(vigencia.suspensionActiva(local).isPresent());

        // La renovación de la licencia se rechaza: el local sigue suspendido.
        SolicitudResponse licenciaNueva = adjuntar(propietario, local, TipoDocumento.LICENCIA_FUNCIONAMIENTO,
                "LF-N", hoy().plusYears(1));
        solicitudes.enviar(propietario, local);
        revision.resolver(admin, licenciaNueva.id(), new ResolucionRequest(List.of(new ResolucionRequest.Documento(
                licenciaNueva.documentos().getFirst().id(), ResultadoRevision.RECHAZADO, "Sin firma")),
                "Falta la firma del alcalde.  "));
        NotificacionService.Aviso rechazo = ultimoAviso(propietario, TipoNotificacion.RESULTADO_AFILIACION);
        assertEquals("Renovación rechazada: Local renovación suspendida", rechazo.titulo());
        assertEquals("Motivo: Falta la firma del alcalde. El local sigue suspendido; presenta nuevamente "
                + "los documentos vencidos.", rechazo.mensaje());

        // Con la licencia renovada y aprobada ya no quedan vencidos: se reactiva.
        SolicitudResponse ultima = adjuntar(propietario, local, TipoDocumento.LICENCIA_FUNCIONAMIENTO,
                "LF-N2", hoy().plusYears(1));
        solicitudes.enviar(propietario, local);
        aprobarTodo(admin, ultima, null);
        assertTrue(ultimoAviso(propietario, TipoNotificacion.RESULTADO_AFILIACION).mensaje()
                .startsWith("El local fue reactivado"));
        assertEquals(EstadoEstablecimiento.ACTIVO, estadoLocal(local));
        assertTrue(vigencia.suspensionActiva(local).isEmpty());
    }

    private static EstablecimientoAdminResponse buscar(List<EstablecimientoAdminResponse> lista, Long id) {
        return lista.stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow();
    }
}
