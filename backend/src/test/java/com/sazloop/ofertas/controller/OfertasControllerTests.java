package com.sazloop.ofertas.controller;

import com.sazloop.ofertas.EscenarioOfertas;
import com.sazloop.ofertas.EscenarioOfertas.LocalPrueba;
import com.sazloop.ofertas.dto.OfertaResponse;
import com.sazloop.shared.Tiempo;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.sazloop.ofertas.EscenarioOfertas.unico;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints de packs, publicaciones, búsqueda pública y panel del local, por HTTP simulado. */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class OfertasControllerTests {
    @Autowired WebApplicationContext contexto;

    MockMvc mvc;
    EscenarioOfertas escenario;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        escenario = new EscenarioOfertas(contexto);
    }

    private static MockHttpSession sesionDe(LocalPrueba local) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("cuentaLocalId", local.cuenta().getId());
        return sesion;
    }

    private static String fecha(LocalDateTime valor) {
        return valor.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    @Test
    void lasRutasDelLocalExigenSesion() throws Exception {
        mvc.perform(get("/api/local/packs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/local/resumen")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/local/packs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"P\",\"descripcion\":\"D\",\"posiblesProductos\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearPackListarloYPublicarSuOferta() throws Exception {
        LocalPrueba local = escenario.localActivo(unico("Http"), "Calle Http");
        MockHttpSession sesion = sesionDe(local);

        String creado = mvc.perform(post("/api/local/packs").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Pack HTTP\",\"descripcion\":\"Panes\",\"posiblesProductos\":\"Pan\","
                                + "\"alergenos\":[\"Trigo\"],\"restricciones\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        String packId = creado.replaceAll("\\D+", "");

        mvc.perform(get("/api/local/packs").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Pack HTTP"));

        LocalDateTime inicio = Tiempo.ahora().plusMinutes(1);
        mvc.perform(post("/api/local/ofertas").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packId\":" + packId + ",\"inicioRecojo\":\"" + fecha(inicio)
                                + "\",\"finRecojo\":\"" + fecha(inicio.plusHours(1)) + "\",\"variantes\":["
                                + "{\"tamanio\":\"PEQUENO\",\"precioReferencial\":20,\"precioDescuento\":12,\"stock\":3}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.pack").value("Pack HTTP"))
                .andExpect(jsonPath("$.variantes[0].ahorro").value(8))
                .andExpect(jsonPath("$.variantes[0].stockDisponible").value(3));

        mvc.perform(get("/api/local/resumen").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ofertaVigente.pack").value("Pack HTTP"))
                .andExpect(jsonPath("$.stockDisponible").value(3))
                .andExpect(jsonPath("$.tokenQr", notNullValue()))
                .andExpect(jsonPath("$.estadoLocal").value("ACTIVO"));
    }

    @Test
    void publicarPackYOfertaJuntosYValidarElCuerpo() throws Exception {
        LocalPrueba local = escenario.localActivo(unico("HttpJunto"), "Calle Junta");
        MockHttpSession sesion = sesionDe(local);
        LocalDateTime inicio = Tiempo.ahora().plusMinutes(1);
        String variantes = "\"variantes\":[{\"tamanio\":\"GRANDE\",\"precioReferencial\":30,"
                + "\"precioDescuento\":15,\"stock\":2}]";

        // Un pack sin nombre no pasa la validación del cuerpo.
        mvc.perform(post("/api/local/publicaciones").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pack\":{\"nombre\":\"\",\"descripcion\":\"D\",\"posiblesProductos\":\"X\"},"
                                + "\"inicioRecojo\":\"" + fecha(inicio) + "\",\"finRecojo\":\""
                                + fecha(inicio.plusHours(1)) + "\"," + variantes + "}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/local/publicaciones").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pack\":{\"nombre\":\"Junto\",\"descripcion\":\"D\",\"posiblesProductos\":\"X\"},"
                                + "\"inicioRecojo\":\"" + fecha(inicio) + "\",\"finRecojo\":\""
                                + fecha(inicio.plusHours(1)) + "\"," + variantes + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pack").value("Junto"))
                .andExpect(jsonPath("$.variantes[0].tamanio").value("GRANDE"));

        // La regla de negocio (una sola oferta activa) llega como 409.
        mvc.perform(post("/api/local/publicaciones").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pack\":{\"nombre\":\"Otro\",\"descripcion\":\"D\",\"posiblesProductos\":\"X\"},"
                                + "\"inicioRecojo\":\"" + fecha(inicio) + "\",\"finRecojo\":\""
                                + fecha(inicio.plusHours(1)) + "\"," + variantes + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void busquedaYFichaPublicasNoRequierenSesion() throws Exception {
        String distrito = unico("HttpBusqueda");
        LocalPrueba local = escenario.localActivo(distrito, "Calle Publica", "-12.100000", "-77.030000");
        OfertaResponse oferta = escenario.publicar(local, 2);

        mvc.perform(get("/api/ofertas").param("distrito", distrito).param("precioMaximo", "15")
                        .param("lat", "-12.1").param("lng", "-77.03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(oferta.id()))
                .andExpect(jsonPath("$[0].distanciaKm").value(0.0));
        mvc.perform(get("/api/ofertas").param("distrito", distrito).param("precioMaximo", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/ofertas").param("distrito", distrito).param("lat", "-12.1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/ofertas").param("calificacionMinima", "7"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/ofertas/{id}", oferta.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distrito").value(distrito))
                .andExpect(jsonPath("$.direccion").value("Calle Publica"));
        mvc.perform(get("/api/ofertas/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }
}
