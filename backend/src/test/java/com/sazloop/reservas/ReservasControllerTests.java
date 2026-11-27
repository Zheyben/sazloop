package com.sazloop.reservas;

import com.sazloop.ofertas.dto.OfertaResponse;
import com.sazloop.reservas.domain.Reserva;
import com.sazloop.reservas.dto.CompraResponse;
import com.sazloop.reservas.service.CompraRecojoService;
import com.sazloop.transacciones.domain.Pago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Endpoints de compras, recojo, cancelación y compra presencial, con la sesión de consumidor o de local. */
class ReservasControllerTests extends EscenarioReservasBase {
    @Autowired WebApplicationContext contexto;
    @Autowired CompraRecojoService compras;
    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private static MockHttpSession sesionConsumidor(Long usuarioId) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("usuarioId", usuarioId);
        return sesion;
    }

    private static MockHttpSession sesionLocal(Local local) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("cuentaLocalId", local.cuenta().getId());
        return sesion;
    }

    private static String compraJson(OfertaResponse oferta, String medio, int cantidad) {
        return "{\"ofertaId\":" + oferta.id() + ",\"medioPago\":\"" + medio + "\",\"lineas\":[{\"varianteId\":"
                + variante(oferta) + ",\"cantidad\":" + cantidad + "}]}";
    }

    private String token(Long ofertaId) {
        return ofertasRepo.findById(ofertaId).orElseThrow().getTokenQr();
    }

    @Test
    void comprarSinSesionDevuelve401YConSesionCrea201() throws Exception {
        OfertaResponse oferta = oferta(local(), 3);
        Long consumidor = usuario();
        mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON)
                        .content(compraJson(oferta, "YAPE", 1)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/compras").session(sesionConsumidor(consumidor)).contentType(MediaType.APPLICATION_JSON)
                        .content(compraJson(oferta, "YAPE", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.medioPago").value("YAPE"))
                .andExpect(jsonPath("$.total").value(8.00))
                .andExpect(jsonPath("$.codigoRecojo").isNotEmpty());
        assertEquals(2, stock(oferta.id()));
        mvc.perform(get("/api/compras/mias").session(sesionConsumidor(consumidor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cancelable").value(true));
    }

    @Test
    void cuerpoInvalidoDevuelve400SinTocarElStock() throws Exception {
        OfertaResponse oferta = oferta(local(), 3);
        Long consumidor = usuario();
        mvc.perform(post("/api/compras").session(sesionConsumidor(consumidor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ofertaId\":" + oferta.id() + ",\"medioPago\":\"YAPE\",\"lineas\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/compras").session(sesionConsumidor(consumidor)).contentType(MediaType.APPLICATION_JSON)
                        .content(compraJson(oferta, "YAPE", 0)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/compras").session(sesionConsumidor(consumidor)).contentType(MediaType.APPLICATION_JSON)
                        .content(compraJson(oferta, "EFECTIVO", 1)))
                .andExpect(status().isBadRequest());
        assertEquals(3, stock(oferta.id()));
    }

    @Test
    void loteCreaUnaCompraPorPedido() throws Exception {
        OfertaResponse oferta = oferta(local(), 3);
        String pedido = "{\"ofertaId\":" + oferta.id() + ",\"lineas\":[{\"varianteId\":" + variante(oferta)
                + ",\"cantidad\":1}]}";
        mvc.perform(post("/api/compras/lote").session(sesionConsumidor(usuario())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medioPago\":\"TARJETA_DEBITO\",\"pedidos\":[" + pedido + "," + pedido + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].medioPago").value("TARJETA_DEBITO"));
        assertEquals(1, stock(oferta.id()));
    }

    @Test
    void elLocalBuscaYConfirmaElRecojoConSuSesion() throws Exception {
        Local local = local();
        OfertaResponse oferta = oferta(local, 2);
        CompraResponse compra = compras.comprar(usuario(), pedido(oferta, Pago.MedioPago.YAPE, 1));
        String codigo = compra.codigoRecojo().replace("-", "").toLowerCase();

        mvc.perform(get("/api/local/recojos/" + codigo)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/local/recojos/" + codigo).session(sesionLocal(local)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservaId").value(compra.reservaId()))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        mvc.perform(get("/api/local/recojos/NOEXISTE0000").session(sesionLocal(local)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/local/recojos/" + codigo + "/confirmar").session(sesionLocal(local)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECOGIDA"));
        mvc.perform(post("/api/local/recojos/" + codigo + "/confirmar").session(sesionLocal(local)))
                .andExpect(status().isConflict());
        assertEquals(Reserva.EstadoReserva.RECOGIDA, estadoReserva(compra.reservaId()));
    }

    @Test
    void cancelarDevuelve204YLaSegundaVez409() throws Exception {
        OfertaResponse oferta = oferta(local(), 2);
        Long consumidor = usuario();
        CompraResponse compra = compras.comprar(consumidor, pedido(oferta, Pago.MedioPago.YAPE, 1));
        mvc.perform(post("/api/compras/" + compra.reservaId() + "/cancelar")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/compras/" + compra.reservaId() + "/cancelar").session(sesionConsumidor(usuario())))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/compras/" + compra.reservaId() + "/cancelar").session(sesionConsumidor(consumidor)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/compras/" + compra.reservaId() + "/cancelar").session(sesionConsumidor(consumidor)))
                .andExpect(status().isConflict());
        assertEquals(Reserva.EstadoReserva.CANCELADA, estadoReserva(compra.reservaId()));
        assertEquals(2, stock(oferta.id()));
    }

    @Test
    void compraPresencialDigitalYEnEfectivoConConfirmacionDelLocal() throws Exception {
        Local local = local();
        OfertaResponse oferta = oferta(local, 3);
        String token = token(oferta.id());
        mvc.perform(post("/api/compras/presenciales").session(sesionConsumidor(usuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"compra\":" + compraJson(oferta, "YAPE", 1) + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cancelable").value(false));
        mvc.perform(post("/api/compras/presenciales").session(sesionConsumidor(usuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\",\"compra\":" + compraJson(oferta, "YAPE", 1) + "}"))
                .andExpect(status().isBadRequest());

        String respuesta = mvc.perform(post("/api/compras/presenciales/efectivo").session(sesionConsumidor(usuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"compra\":" + compraJson(oferta, "EFECTIVO", 1) + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(8.00))
                .andExpect(jsonPath("$.venceEn").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        assertEquals(1, stock(oferta.id()));
        String codigo = respuesta.replaceAll(".*\"codigoRecojo\":\"([^\"]+)\".*", "$1");

        mvc.perform(post("/api/local/efectivo/" + codigo + "/confirmar")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/local/efectivo/" + codigo + "/confirmar").session(sesionLocal(local)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECOGIDA"))
                .andExpect(jsonPath("$.medioPago").value("EFECTIVO"));
        mvc.perform(post("/api/local/efectivo/" + codigo + "/confirmar").session(sesionLocal(local)))
                .andExpect(status().isConflict());
    }
}
