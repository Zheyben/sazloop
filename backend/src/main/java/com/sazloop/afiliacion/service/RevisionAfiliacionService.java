package com.sazloop.afiliacion.service;

import com.sazloop.afiliacion.domain.*;
import com.sazloop.afiliacion.domain.EvaluacionAfiliacion.DecisionAfiliacion;
import com.sazloop.afiliacion.domain.RevisionDocumento.ResultadoRevision;
import com.sazloop.afiliacion.domain.SolicitudAfiliacion.EstadoSolicitud;
import com.sazloop.afiliacion.dto.*;
import com.sazloop.afiliacion.repository.*;
import com.sazloop.experiencia.domain.Notificacion.TipoNotificacion;
import com.sazloop.experiencia.service.NotificacionService;
import com.sazloop.identidad.domain.Establecimiento;
import com.sazloop.identidad.repository.EstablecimientoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Revisión humana de cada archivo y resolución de la solicitud o renovación (RF-07, RF-08 y RF-09). */
@Service
public class RevisionAfiliacionService {
    private static final String MOTIVO = "Motivo: ";
    private final SolicitudAfiliacionRepository solicitudes;
    private final DocumentoEstablecimientoRepository documentos;
    private final RevisionDocumentoRepository revisiones;
    private final EvaluacionAfiliacionRepository evaluaciones;
    private final AlmacenDocumentoService almacen;
    private final EstablecimientoRepository establecimientos;
    private final VigenciaDocumentosService vigencia;
    private final NotificacionService notificaciones;

    public RevisionAfiliacionService(SolicitudAfiliacionRepository solicitudes,
            DocumentoEstablecimientoRepository documentos, RevisionDocumentoRepository revisiones,
            EvaluacionAfiliacionRepository evaluaciones, AlmacenDocumentoService almacen,
            EstablecimientoRepository establecimientos, VigenciaDocumentosService vigencia,
            NotificacionService notificaciones) {
        this.solicitudes = solicitudes;
        this.documentos = documentos;
        this.revisiones = revisiones;
        this.evaluaciones = evaluaciones;
        this.almacen = almacen;
        this.establecimientos = establecimientos;
        this.vigencia = vigencia;
        this.notificaciones = notificaciones;
    }

    @Transactional(readOnly = true)
    public List<SolicitudRevisionResponse> pendientes() {
        return solicitudes.findByEstadoOrderByIdAsc(EstadoSolicitud.EN_REVISION)
                .stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public SolicitudRevisionResponse consultar(Long id) { return respuesta(solicitud(id)); }

    /** Todos los locales con su estado; los suspendidos muestran el motivo. */
    @Transactional(readOnly = true)
    public List<EstablecimientoAdminResponse> establecimientos() {
        return establecimientos.findAll(Sort.by("id")).stream().map(local -> {
            var suspension = vigencia.suspensionActiva(local.getId()).orElse(null);
            return new EstablecimientoAdminResponse(local.getId(), local.getNombre(),
                    local.getEmpresa().getNombreComercial(), local.getEmpresa().getRuc(),
                    local.getDireccion().getDistrito(), local.getDireccion().getDireccionLinea(),
                    local.getEstado(), suspension == null ? null : suspension.getMotivo(),
                    suspension == null ? null : suspension.getFechaInicio(),
                    suspension != null && VigenciaDocumentosService.CORREO_SISTEMA
                            .equalsIgnoreCase(suspension.getAdministrador().getEmailInstitucional()),
                    solicitudes.existsByEstablecimiento_IdAndEstado(local.getId(), EstadoSolicitud.EN_REVISION));
        }).toList();
    }

    @Transactional(readOnly = true)
    public SolicitudAfiliacionService.ArchivoPrivado descargar(Long documentoId) {
        DocumentoEstablecimiento doc = documento(documentoId);
        String nombre = doc.getUrlArchivo();
        return new SolicitudAfiliacionService.ArchivoPrivado(
                almacen.leer(nombre), nombre.substring(nombre.lastIndexOf('.') + 1));
    }

    @Transactional
    public SolicitudRevisionResponse revisar(AdministradorPlataforma admin,
            Long documentoId, RevisionRequest entrada) {
        DocumentoEstablecimiento doc = documento(documentoId);
        SolicitudAfiliacion solicitud = doc.getSolicitudAfiliacion();
        exigirEnRevision(solicitud);
        if (revisiones.findFirstByDocumento_IdOrderByIdDesc(documentoId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El documento ya fue revisado");
        }
        if (entrada.resultado() == ResultadoRevision.RECHAZADO &&
                (entrada.observaciones() == null || entrada.observaciones().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica las observaciones del rechazo");
        }
        if (entrada.tipo() != null) doc.setTipoDocumento(entrada.tipo());
        if (entrada.numero() != null) doc.setNumeroDocumento(entrada.numero().isBlank() ? null : entrada.numero().trim());
        if (entrada.emision() != null) doc.setFechaEmision(entrada.emision());
        if (entrada.vencimiento() != null) doc.setFechaVencimiento(entrada.vencimiento());
        if (doc.getFechaEmision() != null && doc.getFechaVencimiento() != null &&
                doc.getFechaVencimiento().isBefore(doc.getFechaEmision()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vencimiento anterior a emisión");
        // Un documento vencido no puede aprobarse, aunque la petición no llegue desde la pantalla.
        // Se revisa después de aplicar la fecha que pudo corregir el administrador.
        if (entrada.resultado() == ResultadoRevision.APROBADO
                && VigenciaDocumentosService.estado(doc, VigenciaDocumentosService.hoy())
                   == VigenciaDocumentosService.EstadoVigencia.VENCIDO)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un documento vencido solo puede rechazarse");
        revisiones.save(RevisionDocumento.builder().documento(doc).administrador(admin)
                .resultado(entrada.resultado()).observaciones(entrada.observaciones()).build());
        return respuesta(solicitud);
    }

    /**
     * Registra la revisión de cada documento y la decisión final de una sola vez. Si algo falla
     * (por ejemplo, aprobar con un documento vencido) no se guarda nada.
     */
    @Transactional
    public SolicitudRevisionResponse resolver(AdministradorPlataforma admin, Long id, ResolucionRequest entrada) {
        SolicitudAfiliacion solicitud = solicitud(id);
        exigirEnRevision(solicitud);
        List<DocumentoEstablecimiento> lista = documentos.findBySolicitudAfiliacion_IdOrderByIdAsc(id);
        Map<Long, ResolucionRequest.Documento> porId = new HashMap<>();
        for (ResolucionRequest.Documento item : entrada.documentos()) porId.put(item.id(), item);
        // Resultado y observación finales de cada documento: se conserva la revisión previa si ya existe.
        Map<Long, ResultadoRevision> resultados = new HashMap<>();
        Map<Long, String> observaciones = new HashMap<>();
        for (DocumentoEstablecimiento doc : lista) {
            registrarResultado(admin, doc, porId.get(doc.getId()), resultados, observaciones);
        }
        boolean todoAprobado = !lista.isEmpty() && lista.stream()
                .allMatch(d -> resultados.get(d.getId()) == ResultadoRevision.APROBADO);
        DecisionAfiliacion decision = todoAprobado ? DecisionAfiliacion.APROBADA : DecisionAfiliacion.RECHAZADA;
        String motivo = motivoResolucion(entrada.comentario(), todoAprobado, lista, resultados, observaciones);
        return decidir(admin, id, new DecisionRequest(decision, motivo));
    }

    /** Anota el resultado de un documento: reutiliza la revisión previa o registra la que llegó en la petición. */
    private void registrarResultado(AdministradorPlataforma admin, DocumentoEstablecimiento doc,
            ResolucionRequest.Documento item, Map<Long, ResultadoRevision> resultados,
            Map<Long, String> observaciones) {
        RevisionDocumento previa = revisiones.findFirstByDocumento_IdOrderByIdDesc(doc.getId()).orElse(null);
        if (previa != null) {
            resultados.put(doc.getId(), previa.getResultado());
            observaciones.put(doc.getId(), Objects.toString(previa.getObservaciones(), ""));
            return;
        }
        if (item == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Indica el resultado de cada documento: aprobar o rechazar");
        revisar(admin, doc.getId(), new RevisionRequest(item.resultado(), item.observaciones()));
        resultados.put(doc.getId(), item.resultado());
        observaciones.put(doc.getId(), Objects.toString(item.observaciones(), ""));
    }

    /** Motivo de la decisión: el comentario del administrador o, si no hay, uno armado con las revisiones. */
    private static String motivoResolucion(String comentario, boolean todoAprobado,
            List<DocumentoEstablecimiento> lista, Map<Long, ResultadoRevision> resultados,
            Map<Long, String> observaciones) {
        String motivo;
        if (comentario != null && !comentario.isBlank()) {
            motivo = comentario.trim();
        } else if (todoAprobado) {
            motivo = "Documentación revisada y conforme";
        } else {
            motivo = lista.stream().filter(d -> resultados.get(d.getId()) == ResultadoRevision.RECHAZADO)
                    .map(d -> d.getTipoDocumento().etiqueta() + ": " + observaciones.get(d.getId()).trim())
                    .collect(Collectors.joining("; "));
        }
        if (motivo.isBlank()) motivo = "Solicitud rechazada tras la revisión de los documentos";
        if (motivo.length() > 500) motivo = motivo.substring(0, 499) + "…";
        return motivo;
    }

    @Transactional
    public SolicitudRevisionResponse decidir(AdministradorPlataforma admin, Long id, DecisionRequest entrada) {
        SolicitudAfiliacion solicitud = solicitud(id);
        exigirEnRevision(solicitud);
        List<DocumentoEstablecimiento> lista = documentos.findBySolicitudAfiliacion_IdOrderByIdAsc(id);
        if (lista.isEmpty() || lista.stream().anyMatch(d ->
                revisiones.findFirstByDocumento_IdOrderByIdDesc(d.getId()).isEmpty())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Revisa todos los documentos antes de decidir");
        }
        if (entrada.decision() == DecisionAfiliacion.APROBADA) {
            boolean noAprobable = lista.stream().anyMatch(d ->
                    revisiones.findFirstByDocumento_IdOrderByIdDesc(d.getId()).orElseThrow().getResultado()
                            != ResultadoRevision.APROBADO ||
                    d.getFechaVencimiento() != null
                            && d.getFechaVencimiento().isBefore(VigenciaDocumentosService.hoy()));
            if (noAprobable) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede aprobar con documentos rechazados o vencidos");
        }
        if (evaluaciones.existsBySolicitudAfiliacion_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya fue resuelta");
        }
        boolean renovacion = esRenovacion(solicitud);
        EvaluacionAfiliacion evaluacion = evaluaciones.save(EvaluacionAfiliacion.builder()
                .solicitudAfiliacion(solicitud).administrador(admin)
                .decision(entrada.decision()).motivo(entrada.motivo().trim()).build());
        solicitud.setEvaluacion(evaluacion);
        boolean aprobada = entrada.decision() == DecisionAfiliacion.APROBADA;
        solicitud.setEstado(aprobada ? EstadoSolicitud.APROBADA : EstadoSolicitud.RECHAZADA);
        Establecimiento local = solicitud.getEstablecimiento();
        boolean reactivado = false;
        if (aprobada && !renovacion) local.setEstado(Establecimiento.EstadoEstablecimiento.ACTIVO);
        if (aprobada && renovacion) reactivado = vigencia.reactivar(local);
        notificarDecision(solicitud, renovacion, aprobada, reactivado, entrada.motivo().trim());
        return respuesta(solicitud);
    }

    private void notificarDecision(SolicitudAfiliacion solicitud, boolean renovacion, boolean aprobada,
            boolean reactivado, String motivo) {
        Establecimiento local = solicitud.getEstablecimiento();
        boolean suspendido = local.getEstado() == Establecimiento.EstadoEstablecimiento.SUSPENDIDO;
        String titulo;
        String mensaje;
        if (!renovacion && aprobada) {
            titulo = "Solicitud aprobada: " + local.getNombre();
            mensaje = "El local ya puede operar en Sazloop. Crea la cuenta del trabajador para que publique "
                    + "su pack sorpresa. " + MOTIVO + motivo;
        } else if (!renovacion) {
            titulo = "Solicitud rechazada: " + local.getNombre();
            mensaje = MOTIVO + sinPuntuacionFinal(motivo) + ". Puedes adjuntar nuevos documentos y enviar otra solicitud.";
        } else if (aprobada) {
            titulo = "Renovación aprobada: " + local.getNombre();
            mensaje = estadoTrasRenovacion(reactivado, suspendido) + MOTIVO + motivo;
        } else {
            titulo = "Renovación rechazada: " + local.getNombre();
            mensaje = MOTIVO + sinPuntuacionFinal(motivo) + ". " + (suspendido
                    ? "El local sigue suspendido; presenta nuevamente los documentos vencidos."
                    : "Presenta nuevamente los documentos antes de que venzan.");
        }
        notificaciones.crear(local.getEmpresa().getPropietario().getUsuario(),
                TipoNotificacion.RESULTADO_AFILIACION, titulo, mensaje, "SolicitudAfiliacion", solicitud.getId());
    }

    /** Frase que explica cómo quedó el local tras aprobarse una renovación. */
    private static String estadoTrasRenovacion(boolean reactivado, boolean suspendido) {
        if (reactivado) return "El local fue reactivado y vuelve a aparecer en la búsqueda. ";
        if (suspendido) return "El local sigue suspendido porque aún tiene documentos vencidos. ";
        return "Los documentos renovados reemplazan a los anteriores. ";
    }

    private boolean esRenovacion(SolicitudAfiliacion s) {
        return solicitudes.existsByEstablecimiento_IdAndEstadoAndIdLessThan(
                s.getEstablecimiento().getId(), EstadoSolicitud.APROBADA, s.getId());
    }

    private SolicitudRevisionResponse respuesta(SolicitudAfiliacion s) {
        List<SolicitudRevisionResponse.DocumentoEnRevision> lista =
                documentos.findBySolicitudAfiliacion_IdOrderByIdAsc(s.getId()).stream().map(d -> {
                    RevisionDocumento r = revisiones.findFirstByDocumento_IdOrderByIdDesc(d.getId()).orElse(null);
                    return new SolicitudRevisionResponse.DocumentoEnRevision(d.getId(),
                            d.getTipoDocumento().name(), d.getNumeroDocumento(), d.getFechaEmision(),
                            d.getFechaVencimiento(), r == null ? null : r.getResultado(),
                            r == null ? null : r.getObservaciones());
                }).toList();
        Establecimiento local = s.getEstablecimiento();
        return new SolicitudRevisionResponse(s.getId(), local.getId(), local.getNombre(), s.getEstado(), lista,
                esRenovacion(s), local.getEstado(), local.getEmpresa().getNombreComercial(),
                local.getEmpresa().getRuc(), local.getDireccion().getDistrito(),
                local.getDireccion().getDireccionLinea(), s.getFechaSolicitud(),
                s.getEvaluacion() == null ? null : s.getEvaluacion().getMotivo());
    }

    private SolicitudAfiliacion solicitud(Long id) {
        return solicitudes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    /** Quita los puntos y espacios del final para que, al añadir ". ", no quede «..». */
    private static String sinPuntuacionFinal(String motivo) {
        return motivo.replaceAll("[.\\s]+$", "");
    }
    private DocumentoEstablecimiento documento(Long id) {
        return documentos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private static void exigirEnRevision(SolicitudAfiliacion solicitud) {
        if (solicitud.getEstado() != EstadoSolicitud.EN_REVISION)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud no está en revisión");
    }
}
