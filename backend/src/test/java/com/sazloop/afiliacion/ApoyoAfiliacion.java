package com.sazloop.afiliacion;

import com.sazloop.afiliacion.domain.AdministradorPlataforma;
import com.sazloop.afiliacion.domain.DocumentoEstablecimiento;
import com.sazloop.afiliacion.domain.DocumentoEstablecimiento.TipoDocumento;
import com.sazloop.afiliacion.domain.RevisionDocumento.ResultadoRevision;
import com.sazloop.afiliacion.dto.ResolucionRequest;
import com.sazloop.afiliacion.dto.SolicitudResponse;
import com.sazloop.afiliacion.repository.AdministradorPlataformaRepository;
import com.sazloop.afiliacion.repository.DocumentoEstablecimientoRepository;
import com.sazloop.afiliacion.repository.SolicitudAfiliacionRepository;
import com.sazloop.afiliacion.service.RevisionAfiliacionService;
import com.sazloop.afiliacion.service.SolicitudAfiliacionService;
import com.sazloop.afiliacion.service.VigenciaDocumentosService;
import com.sazloop.experiencia.domain.Notificacion.TipoNotificacion;
import com.sazloop.experiencia.service.NotificacionService;
import com.sazloop.identidad.domain.Establecimiento;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import com.sazloop.identidad.domain.Usuario;
import com.sazloop.identidad.dto.DireccionRequest;
import com.sazloop.identidad.dto.EmpresaRequest;
import com.sazloop.identidad.dto.EstablecimientoRequest;
import com.sazloop.identidad.repository.EstablecimientoRepository;
import com.sazloop.identidad.repository.UsuarioRepository;
import com.sazloop.identidad.service.EmpresaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Datos y pasos comunes de las pruebas de afiliación: cada prueba crea su propio propietario,
 * empresa (con RUC aleatorio) y local, por lo que no depende de los datos de demostración
 * ni del orden de ejecución.
 */
public abstract class ApoyoAfiliacion {
    protected static final byte[] PDF = "%PDF-1.4\nPrueba de afiliación".getBytes(StandardCharsets.UTF_8);

    @Autowired protected UsuarioRepository usuarios;
    @Autowired protected EmpresaService empresas;
    @Autowired protected SolicitudAfiliacionService solicitudes;
    @Autowired protected RevisionAfiliacionService revision;
    @Autowired protected VigenciaDocumentosService vigencia;
    @Autowired protected NotificacionService notificaciones;
    @Autowired protected AdministradorPlataformaRepository administradores;
    @Autowired protected DocumentoEstablecimientoRepository documentosRepo;
    @Autowired protected SolicitudAfiliacionRepository solicitudesRepo;
    @Autowired protected EstablecimientoRepository establecimientosRepo;

    protected static LocalDate hoy() {
        return VigenciaDocumentosService.hoy();
    }

    protected Long usuario() {
        return usuarios.save(Usuario.builder().nombres("Prueba").apellidos("Afiliación")
                .email(UUID.randomUUID() + "@example.test").passwordHash("hash-temporal").build()).getId();
    }

    /** Registra la empresa del propietario (si hace falta) y un local pendiente de aprobación. */
    protected Long localPendiente(Long propietario, String nombre) {
        String ruc = Long.toString(ThreadLocalRandom.current().nextLong(10_000_000_000L, 100_000_000_000L));
        if (empresas.obtenerEmpresa(propietario).isEmpty()) {
            empresas.registrarEmpresa(propietario, new EmpresaRequest("Empresa " + nombre, "Comercio", ruc));
        }
        return empresas.registrarEstablecimiento(propietario, new EstablecimientoRequest(nombre, null, null,
                new DireccionRequest("Lima", "Lima", "Jesús María", "Av. de prueba 123", null, null, null))).id();
    }

    protected SolicitudResponse adjuntar(Long propietario, Long local, TipoDocumento tipo, String numero,
                                         LocalDate vencimiento) {
        return solicitudes.adjuntar(propietario, local, tipo, numero, null, vencimiento, pdf());
    }

    /** Local pendiente con los documentos indicados (vencen en un año) y la solicitud ya enviada. */
    protected SolicitudResponse solicitudEnRevision(Long propietario, Long local, TipoDocumento... tipos) {
        SolicitudResponse ultima = null;
        for (TipoDocumento tipo : tipos) {
            ultima = adjuntar(propietario, local, tipo, tipo.name() + "-1", hoy().plusYears(1));
        }
        solicitudes.enviar(propietario, local);
        return ultima;
    }

    /** Aprueba todos los documentos de la solicitud en un solo paso. */
    protected void aprobarTodo(AdministradorPlataforma admin, SolicitudResponse solicitud, String comentario) {
        List<ResolucionRequest.Documento> lista = solicitud.documentos().stream()
                .map(d -> new ResolucionRequest.Documento(d.id(), ResultadoRevision.APROBADO, null)).toList();
        revision.resolver(admin, solicitud.id(), new ResolucionRequest(lista, comentario));
    }

    /** Local ya aprobado y activo, con un documento de cada tipo indicado. */
    protected SolicitudResponse localAprobado(Long propietario, Long local, AdministradorPlataforma admin,
                                              TipoDocumento... tipos) {
        SolicitudResponse solicitud = solicitudEnRevision(propietario, local, tipos);
        aprobarTodo(admin, solicitud, null);
        return solicitud;
    }

    protected void cambiarVencimiento(Long documentoId, LocalDate vencimiento) {
        DocumentoEstablecimiento doc = documentosRepo.findById(documentoId).orElseThrow();
        doc.setFechaVencimiento(vencimiento);
        documentosRepo.saveAndFlush(doc);
    }

    protected void cambiarEstadoLocal(Long local, EstadoEstablecimiento estado) {
        Establecimiento entidad = establecimientosRepo.findById(local).orElseThrow();
        entidad.setEstado(estado);
        establecimientosRepo.saveAndFlush(entidad);
    }

    protected EstadoEstablecimiento estadoLocal(Long local) {
        return establecimientosRepo.findById(local).orElseThrow().getEstado();
    }

    protected AdministradorPlataforma admin() {
        return administradores.save(AdministradorPlataforma.builder().nombre("Revisor de prueba")
                .emailInstitucional(UUID.randomUUID() + "@example.test")
                .passwordHash("hash-no-usado").build());
    }

    /** Último aviso del tipo indicado que recibió el usuario. */
    protected NotificacionService.Aviso ultimoAviso(Long usuarioId, TipoNotificacion tipo) {
        return notificaciones.listar(usuarioId).stream().filter(a -> a.tipo() == tipo).findFirst().orElseThrow();
    }

    protected static MockMultipartFile pdf() {
        return new MockMultipartFile("archivo", "documento.pdf", "application/pdf", PDF);
    }
}
