package com.sazloop.experiencia.service;

import com.sazloop.experiencia.domain.Notificacion;
import com.sazloop.experiencia.domain.Notificacion.TipoNotificacion;
import com.sazloop.experiencia.repository.NotificacionRepository;
import com.sazloop.identidad.domain.Usuario;
import com.sazloop.identidad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Avisos internos: recorte de textos largos, avisos únicos y marcado como leído. */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class NotificacionServiceTests {
    @Autowired NotificacionService avisos;
    @Autowired NotificacionRepository avisosRepo;
    @Autowired UsuarioRepository usuarios;

    private Usuario usuario() {
        return usuarios.save(Usuario.builder().nombres("Prueba").apellidos("Avisos")
                .email(UUID.randomUUID() + "@example.test").passwordHash("hash-temporal").build());
    }

    private List<Notificacion> guardados(Usuario usuario) {
        return avisosRepo.findByDestinatario_IdOrderByIdDesc(usuario.getId());
    }

    @Test
    void crearRecortaTituloYMensajeDemasiadoLargos() {
        Usuario usuario = usuario();
        avisos.crear(usuario, TipoNotificacion.GENERAL, "T".repeat(130), "M".repeat(1001), "PRUEBA", 1L);

        Notificacion aviso = guardados(usuario).getFirst();
        assertEquals(120, aviso.getTitulo().length());
        assertEquals("T".repeat(119) + "…", aviso.getTitulo());
        assertEquals(1000, aviso.getMensaje().length());
        assertTrue(aviso.getMensaje().endsWith("M…"));
        assertEquals("PRUEBA", aviso.getTipoReferencia());
        assertEquals(1L, aviso.getReferenciaId());
        assertFalse(aviso.isLeida());
    }

    @Test
    void crearConservaTextosQueCabenJusto() {
        Usuario usuario = usuario();
        String titulo = "T".repeat(120);
        String mensaje = "M".repeat(1000);
        avisos.crear(usuario, TipoNotificacion.SUSPENSION, titulo, mensaje, null, null);

        Notificacion aviso = guardados(usuario).getFirst();
        assertEquals(titulo, aviso.getTitulo());
        assertEquals(mensaje, aviso.getMensaje());
        assertEquals(TipoNotificacion.SUSPENSION, aviso.getTipo());
    }

    @Test
    void crearUnaVezNoDuplicaElAvisoDelMismoRegistro() {
        Usuario usuario = usuario();
        avisos.crearUnaVez(usuario, TipoNotificacion.RECORDATORIO_RECOJO, "Recoge", "Tu pedido", "RESERVA", 7L);
        avisos.crearUnaVez(usuario, TipoNotificacion.RECORDATORIO_RECOJO, "Recoge", "Tu pedido", "RESERVA", 7L);
        assertEquals(1, guardados(usuario).size());

        // Otro registro u otro tipo sí generan un aviso nuevo.
        avisos.crearUnaVez(usuario, TipoNotificacion.RECORDATORIO_RECOJO, "Recoge", "Otro pedido", "RESERVA", 8L);
        avisos.crearUnaVez(usuario, TipoNotificacion.CAMBIO_RESERVA, "Cambio", "Tu pedido", "RESERVA", 7L);
        assertEquals(3, guardados(usuario).size());
    }

    @Test
    void listarDevuelveLosAvisosDelUsuarioDelMasNuevoAlMasAntiguo() {
        Usuario usuario = usuario();
        Usuario otro = usuario();
        avisos.crear(usuario, TipoNotificacion.GENERAL, "Primero", "Uno", null, null);
        avisos.crear(usuario, TipoNotificacion.GENERAL, "Segundo", "Dos", null, null);
        avisos.crear(otro, TipoNotificacion.GENERAL, "Ajeno", "Tres", null, null);

        List<NotificacionService.Aviso> lista = avisos.listar(usuario.getId());
        assertEquals(List.of("Segundo", "Primero"), lista.stream().map(NotificacionService.Aviso::titulo).toList());
        assertTrue(lista.stream().noneMatch(NotificacionService.Aviso::leida));
        assertNotNull(lista.getFirst().fecha());
        assertTrue(avisos.listar(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void leerPorReferenciaSoloMarcaLosAvisosDeEseRegistro() {
        Usuario usuario = usuario();
        avisos.crear(usuario, TipoNotificacion.RECORDATORIO_RECOJO, "A", "A", "RESERVA", 10L);
        avisos.crear(usuario, TipoNotificacion.RECORDATORIO_RECOJO, "B", "B", "RESERVA", 11L);

        avisos.leerPorReferencia(usuario.getId(), TipoNotificacion.RECORDATORIO_RECOJO, "RESERVA", 10L);
        Notificacion leido = guardados(usuario).stream().filter(a -> a.getReferenciaId() == 10L).findFirst().orElseThrow();
        Notificacion pendiente = guardados(usuario).stream().filter(a -> a.getReferenciaId() == 11L).findFirst().orElseThrow();
        assertTrue(leido.isLeida());
        assertNotNull(leido.getFechaLectura());
        assertFalse(pendiente.isLeida());

        // Repetirlo no cambia la fecha de lectura ya registrada.
        LocalDateTime primeraLectura = leido.getFechaLectura();
        avisos.leerPorReferencia(usuario.getId(), TipoNotificacion.RECORDATORIO_RECOJO, "RESERVA", 10L);
        assertEquals(primeraLectura, avisosRepo.findById(leido.getId()).orElseThrow().getFechaLectura());
    }

    @Test
    void leerTodasMarcaSoloLasPendientesDelUsuario() {
        Usuario usuario = usuario();
        Usuario otro = usuario();
        avisos.crear(usuario, TipoNotificacion.GENERAL, "Uno", "Uno", null, null);
        avisos.crear(usuario, TipoNotificacion.GENERAL, "Dos", "Dos", null, null);
        avisos.crear(otro, TipoNotificacion.GENERAL, "Ajeno", "Ajeno", null, null);
        Notificacion yaLeido = guardados(usuario).getLast();
        avisos.leer(usuario.getId(), yaLeido.getId());
        LocalDateTime primeraLectura = avisosRepo.findById(yaLeido.getId()).orElseThrow().getFechaLectura();

        avisos.leerTodas(usuario.getId());

        assertTrue(guardados(usuario).stream().allMatch(a -> a.isLeida() && a.getFechaLectura() != null));
        assertEquals(primeraLectura, avisosRepo.findById(yaLeido.getId()).orElseThrow().getFechaLectura());
        assertFalse(guardados(otro).getFirst().isLeida());
    }

    @Test
    void leerUnAvisoValidaQueExistaYSeaDelUsuario() {
        Usuario usuario = usuario();
        Usuario otro = usuario();
        avisos.crear(usuario, TipoNotificacion.GENERAL, "Mio", "Mio", null, null);
        Long id = guardados(usuario).getFirst().getId();

        ResponseStatusException inexistente = assertThrows(ResponseStatusException.class,
                () -> avisos.leer(usuario.getId(), Long.MAX_VALUE));
        assertEquals(HttpStatus.NOT_FOUND, inexistente.getStatusCode());
        ResponseStatusException ajeno = assertThrows(ResponseStatusException.class,
                () -> avisos.leer(otro.getId(), id));
        assertEquals(HttpStatus.NOT_FOUND, ajeno.getStatusCode());
        assertFalse(avisosRepo.findById(id).orElseThrow().isLeida());

        avisos.leer(usuario.getId(), id);
        Notificacion leido = avisosRepo.findById(id).orElseThrow();
        assertTrue(leido.isLeida());
        LocalDateTime fecha = leido.getFechaLectura();
        assertNotNull(fecha);

        // Leerlo de nuevo no cambia la fecha de lectura.
        avisos.leer(usuario.getId(), id);
        assertEquals(fecha, avisosRepo.findById(id).orElseThrow().getFechaLectura());
    }
}
