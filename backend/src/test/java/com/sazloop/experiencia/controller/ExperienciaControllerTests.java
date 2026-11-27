package com.sazloop.experiencia.controller;

import com.sazloop.experiencia.domain.Notificacion;
import com.sazloop.experiencia.domain.Notificacion.TipoNotificacion;
import com.sazloop.experiencia.repository.NotificacionRepository;
import com.sazloop.experiencia.repository.ResenaRepository;
import com.sazloop.experiencia.service.NotificacionService;
import com.sazloop.identidad.repository.UsuarioRepository;
import com.sazloop.ofertas.EscenarioOfertas;
import com.sazloop.ofertas.EscenarioOfertas.LocalPrueba;
import com.sazloop.ofertas.dto.OfertaResponse;
import com.sazloop.reservas.dto.CompraResponse;
import com.sazloop.reservas.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static com.sazloop.ofertas.EscenarioOfertas.unico;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints de avisos y reseñas, por HTTP simulado con la sesión de la persona. */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class ExperienciaControllerTests {
    @Autowired WebApplicationContext contexto;
    @Autowired NotificacionService avisos;
    @Autowired NotificacionRepository avisosRepo;
    @Autowired UsuarioRepository usuarios;
    @Autowired ResenaRepository resenasRepo;
    @Autowired ReservaRepository reservasRepo;

    MockMvc mvc;
    EscenarioOfertas escenario;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        escenario = new EscenarioOfertas(contexto);
    }

    private static MockHttpSession sesionDe(Long usuarioId) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("usuarioId", usuarioId);
        return sesion;
    }

    @Test
    void avisosYResenasExigenSesion() throws Exception {
        mvc.perform(get("/api/notificaciones")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/notificaciones/leer-todas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/notificaciones/1/leer")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/compras/1/resena/omitir")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/compras/1/resena").contentType(MediaType.APPLICATION_JSON)
                .content("{\"calificacion\":5}")).andExpect(status().isUnauthorized());
        // Una sesión de un usuario que ya no existe tampoco sirve.
        mvc.perform(get("/api/notificaciones").session(sesionDe(Long.MAX_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarYMarcarAvisosComoLeidos() throws Exception {
        Long usuarioId = escenario.usuario();
        var usuario = usuarios.findById(usuarioId).orElseThrow();
        avisos.crear(usuario, TipoNotificacion.GENERAL, "Primero", "Hola", null, null);
        avisos.crear(usuario, TipoNotificacion.GENERAL, "Segundo", "Chau", null, null);
        MockHttpSession sesion = sesionDe(usuarioId);

        mvc.perform(get("/api/notificaciones").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].titulo").value("Segundo"))
                .andExpect(jsonPath("$[0].leida").value(false));

        Long primero = avisosRepo.findByDestinatario_IdOrderByIdDesc(usuarioId).getLast().getId();
        mvc.perform(post("/api/notificaciones/{id}/leer", primero).session(sesion))
                .andExpect(status().isNoContent());
        assertTrue(avisosRepo.findById(primero).orElseThrow().isLeida());
        // El aviso de otra persona no se puede marcar.
        mvc.perform(post("/api/notificaciones/{id}/leer", primero).session(sesionDe(escenario.usuario())))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/notificaciones/leer-todas").session(sesion)).andExpect(status().isNoContent());
        List<Notificacion> todos = avisosRepo.findByDestinatario_IdOrderByIdDesc(usuarioId);
        assertTrue(todos.stream().allMatch(Notificacion::isLeida));
    }

    @Test
    void registrarYOmitirResenasPorHttp() throws Exception {
        LocalPrueba local = escenario.localActivo(unico("HttpResena"), "Calle Resena");
        OfertaResponse oferta = escenario.publicar(local, 5);
        Long consumidor = escenario.usuario();
        Long otroConsumidor = escenario.usuario();
        CompraResponse compra = escenario.comprarYRecoger(consumidor, oferta, local);
        CompraResponse compraOmitida = escenario.comprarYRecoger(otroConsumidor, oferta, local);

        // La calificación debe estar entre 1 y 5.
        mvc.perform(post("/api/compras/{id}/resena", compra.reservaId()).session(sesionDe(consumidor))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"calificacion\":6}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/compras/{id}/resena", compra.reservaId()).session(sesionDe(consumidor))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"calificacion\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/compras/{id}/resena", compra.reservaId()).session(sesionDe(consumidor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calificacion\":3,\"comentario\":\"" + "x".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
        assertFalse(resenasRepo.existsByReserva_Id(compra.reservaId()));

        mvc.perform(post("/api/compras/{id}/resena", compra.reservaId()).session(sesionDe(consumidor))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"calificacion\":5,\"comentario\":\"Rico\"}"))
                .andExpect(status().isNoContent());
        assertTrue(resenasRepo.existsByReserva_Id(compra.reservaId()));
        mvc.perform(post("/api/compras/{id}/resena", compra.reservaId()).session(sesionDe(consumidor))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"calificacion\":4}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/compras/{id}/resena/omitir", compraOmitida.reservaId()).session(sesionDe(otroConsumidor)))
                .andExpect(status().isNoContent());
        assertTrue(reservasRepo.findById(compraOmitida.reservaId()).orElseThrow().isResenaOmitida());
        mvc.perform(post("/api/compras/{id}/resena/omitir", compraOmitida.reservaId()).session(sesionDe(consumidor)))
                .andExpect(status().isNotFound());
    }
}
