package com.sazloop.reservas;

import com.sazloop.afiliacion.domain.AdministradorPlataforma;
import com.sazloop.afiliacion.domain.DocumentoEstablecimiento.TipoDocumento;
import com.sazloop.afiliacion.domain.RevisionDocumento.ResultadoRevision;
import com.sazloop.afiliacion.dto.ResolucionRequest;
import com.sazloop.afiliacion.repository.AdministradorPlataformaRepository;
import com.sazloop.afiliacion.service.RevisionAfiliacionService;
import com.sazloop.afiliacion.service.SolicitudAfiliacionService;
import com.sazloop.experiencia.domain.Notificacion.TipoNotificacion;
import com.sazloop.experiencia.service.NotificacionService;
import com.sazloop.identidad.domain.CuentaEstablecimiento;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import com.sazloop.identidad.domain.Usuario;
import com.sazloop.identidad.dto.CuentaLocalRequest;
import com.sazloop.identidad.dto.DireccionRequest;
import com.sazloop.identidad.dto.EmpresaRequest;
import com.sazloop.identidad.dto.EstablecimientoRequest;
import com.sazloop.identidad.repository.ConsumidorRepository;
import com.sazloop.identidad.repository.EstablecimientoRepository;
import com.sazloop.identidad.repository.UsuarioRepository;
import com.sazloop.identidad.service.CodificadorContrasena;
import com.sazloop.identidad.service.CuentaLocalService;
import com.sazloop.identidad.service.EmpresaService;
import com.sazloop.ofertas.domain.OfertaPack;
import com.sazloop.ofertas.domain.VariantePack;
import com.sazloop.ofertas.domain.VariantePack.TamanioVariante;
import com.sazloop.ofertas.dto.OfertaRequest;
import com.sazloop.ofertas.dto.OfertaResponse;
import com.sazloop.ofertas.dto.PackRequest;
import com.sazloop.ofertas.dto.PublicacionRequest;
import com.sazloop.ofertas.repository.OfertaPackRepository;
import com.sazloop.ofertas.service.OfertaService;
import com.sazloop.reservas.domain.Reserva;
import com.sazloop.reservas.dto.CompraRequest;
import com.sazloop.reservas.repository.ReservaRepository;
import com.sazloop.reservas.repository.RetencionStockRepository;
import com.sazloop.shared.Tiempo;
import com.sazloop.transacciones.domain.Pago;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Base de las pruebas de integración de reservas: crea con datos únicos un local aprobado,
 * su cuenta de trabajador y una oferta publicada, sobre la base H2 real en memoria.
 */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
public abstract class EscenarioReservasBase {
    @Autowired protected UsuarioRepository usuarios;
    @Autowired protected ConsumidorRepository consumidores;
    @Autowired protected EmpresaService empresas;
    @Autowired protected SolicitudAfiliacionService solicitudes;
    @Autowired protected RevisionAfiliacionService revision;
    @Autowired protected AdministradorPlataformaRepository administradores;
    @Autowired protected CodificadorContrasena contrasenas;
    @Autowired protected CuentaLocalService cuentas;
    @Autowired protected EstablecimientoRepository establecimientos;
    @Autowired protected OfertaService ofertas;
    @Autowired protected OfertaPackRepository ofertasRepo;
    @Autowired protected ReservaRepository reservasRepo;
    @Autowired protected RetencionStockRepository retencionesRepo;
    @Autowired protected NotificacionService notificaciones;
    @Autowired protected TransactionTemplate transacciones;

    /** Un local aprobado con su cuenta de trabajador. */
    protected record Local(Long propietario, Long id, CuentaEstablecimiento cuenta) {}

    protected Long usuario() {
        return usuario("Prueba", "Reservas");
    }

    protected Long usuario(String nombres, String apellidos) {
        return usuarios.save(Usuario.builder().nombres(nombres).apellidos(apellidos)
                .email(UUID.randomUUID() + "@example.test").passwordHash("hash-temporal").build()).getId();
    }

    protected Local local() {
        Long propietario = usuario();
        String ruc = Long.toString(ThreadLocalRandom.current().nextLong(10_000_000_000L, 100_000_000_000L));
        empresas.registrarEmpresa(propietario, new EmpresaRequest("Empresa", "Comercio", ruc));
        Long localId = empresas.registrarEstablecimiento(propietario, new EstablecimientoRequest(
                "Local " + UUID.randomUUID().toString().substring(0, 6), null, null,
                new DireccionRequest("Lima", "Lima", "Lince", "Av. Prueba 123", null, null, null))).id();
        var solicitud = solicitudes.adjuntar(propietario, localId, TipoDocumento.CERTIFICADO_SANITARIO, "CS-R",
                null, Tiempo.hoy().plusYears(1), new MockMultipartFile("archivo", "doc.pdf", "application/pdf",
                        "%PDF-1.4\nPrueba".getBytes(StandardCharsets.UTF_8)));
        solicitudes.enviar(propietario, localId);
        AdministradorPlataforma admin = administradores.save(AdministradorPlataforma.builder().nombre("Revisor")
                .emailInstitucional(UUID.randomUUID() + "@example.test")
                .passwordHash(contrasenas.codificar("clave-segura")).build());
        revision.resolver(admin, solicitud.id(), new ResolucionRequest(List.of(new ResolucionRequest.Documento(
                solicitud.documentos().getFirst().id(), ResultadoRevision.APROBADO, null)), null));
        CuentaEstablecimiento cuenta = cuentas.crear(propietario, localId,
                new CuentaLocalRequest("reservas-" + UUID.randomUUID(), "clave-local"));
        return new Local(propietario, localId, cuenta);
    }

    /** Publica una oferta de dos horas con las variantes indicadas (precio con descuento 10.00, 8.00 o 6.00). */
    protected OfertaResponse oferta(Local local, int stockMediano) {
        return oferta(local, List.of(new OfertaRequest.VarianteRequest(TamanioVariante.MEDIANO,
                new BigDecimal("20.00"), new BigDecimal("8.00"), stockMediano)));
    }

    protected OfertaResponse oferta(Local local, List<OfertaRequest.VarianteRequest> variantes) {
        return ofertas.crearYPublicar(local.cuenta(), new PublicacionRequest(
                new PackRequest("Pack reservas", "Excedentes", "Pan", List.of(), List.of()),
                Tiempo.ahora().plusMinutes(1), Tiempo.ahora().plusHours(2), variantes));
    }

    protected static Long variante(OfertaResponse oferta) {
        return oferta.variantes().getFirst().id();
    }

    protected static CompraRequest pedido(OfertaResponse oferta, Pago.MedioPago medio, int cantidad) {
        return new CompraRequest(oferta.id(), medio, List.of(new CompraRequest.Linea(variante(oferta), cantidad)));
    }

    protected int stock(Long ofertaId) {
        // Se lee del repositorio: la ficha pública ya no muestra ofertas vencidas.
        return transacciones.execute(status -> ofertasRepo.findById(ofertaId).orElseThrow()
                .getVariantes().getFirst().getStockDisponible());
    }

    protected void cambiarOferta(Long ofertaId, java.util.function.Consumer<OfertaPack> cambio) {
        OfertaPack oferta = ofertasRepo.findById(ofertaId).orElseThrow();
        cambio.accept(oferta);
        ofertasRepo.saveAndFlush(oferta);
    }

    protected void finRecojo(Long ofertaId, LocalDateTime fin) {
        cambiarOferta(ofertaId, o -> o.setFinRecojo(fin));
    }

    protected void desactivarVariante(Long ofertaId) {
        transacciones.executeWithoutResult(status -> {
            OfertaPack oferta = ofertasRepo.findById(ofertaId).orElseThrow();
            oferta.getVariantes().forEach(v -> v.setActiva(false));
        });
    }

    protected void estadoLocal(Long localId, EstadoEstablecimiento estado) {
        var local = establecimientos.findById(localId).orElseThrow();
        local.setEstado(estado);
        establecimientos.saveAndFlush(local);
    }

    protected void desactivarUsuario(Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow();
        usuario.setActivo(false);
        usuarios.saveAndFlush(usuario);
    }

    /** Crea el consumidor del usuario ya desactivado (bloqueado por la plataforma). */
    protected void consumidorBloqueado(Long usuarioId) {
        consumidores.saveAndFlush(com.sazloop.identidad.domain.Consumidor.builder()
                .usuario(usuarios.findById(usuarioId).orElseThrow()).activo(false).build());
    }

    protected <T> T enTransaccion(Long reservaId, Function<Reserva, T> lectura) {
        return transacciones.execute(status -> lectura.apply(reservasRepo.findById(reservaId).orElseThrow()));
    }

    protected Reserva.EstadoReserva estadoReserva(Long reservaId) {
        return reservasRepo.findById(reservaId).orElseThrow().getEstado();
    }

    protected List<NotificacionService.Aviso> avisos(Long usuarioId, TipoNotificacion tipo) {
        return notificaciones.listar(usuarioId).stream().filter(a -> a.tipo() == tipo).toList();
    }

    /** Ejecuta la acción, exige un ResponseStatusException con el estado indicado y lo devuelve. */
    protected static ResponseStatusException falla(HttpStatus estado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(estado, error.getStatusCode());
        return error;
    }

    protected static void falla(HttpStatus estado, String mensaje, Executable accion) {
        assertEquals(mensaje, falla(estado, accion).getReason());
    }

    protected static OfertaRequest.VarianteRequest varianteRequest(TamanioVariante tamanio, String precio, int stock) {
        return new OfertaRequest.VarianteRequest(tamanio, new BigDecimal("20.00"), new BigDecimal(precio), stock);
    }

    protected VariantePack.TamanioVariante tamanio(OfertaResponse oferta, int indice) {
        return oferta.variantes().get(indice).tamanio();
    }
}
