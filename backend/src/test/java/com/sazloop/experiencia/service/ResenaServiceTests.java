package com.sazloop.experiencia.service;

import com.sazloop.experiencia.domain.Resena;
import com.sazloop.experiencia.dto.ResenaRequest;
import com.sazloop.experiencia.repository.ResenaRepository;
import com.sazloop.ofertas.EscenarioOfertas;
import com.sazloop.ofertas.EscenarioOfertas.LocalPrueba;
import com.sazloop.ofertas.dto.OfertaResponse;
import com.sazloop.reservas.dto.CompraResponse;
import com.sazloop.reservas.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.sazloop.ofertas.EscenarioOfertas.unico;
import static org.junit.jupiter.api.Assertions.*;

/** Reseñas: solo tras el recojo, una por compra y por local, y la opción de omitirla. */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class ResenaServiceTests {
    @Autowired ApplicationContext contexto;
    @Autowired ResenaService resenas;
    @Autowired ResenaRepository resenasRepo;
    @Autowired ReservaRepository reservasRepo;

    EscenarioOfertas escenario;
    LocalPrueba local;
    OfertaResponse oferta;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioOfertas(contexto);
        local = escenario.localActivo(unico("Resenas"), "Calle Resenas");
        oferta = escenario.publicar(local, 10);
    }

    private static void esperarEstado(HttpStatus estado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(estado, error.getStatusCode());
    }

    private List<Resena> resenasDelLocal() {
        return resenasRepo.findByEstablecimiento_IdAndVisibleTrue(local.localId());
    }

    @Test
    void registrarGuardaLaCalificacionYRecortaElComentario() {
        Long consumidor = escenario.usuario();
        CompraResponse compra = escenario.comprarYRecoger(consumidor, oferta, local);

        resenas.registrar(consumidor, compra.reservaId(), new ResenaRequest(4, "  Muy rico  "));

        List<Resena> guardadas = resenasDelLocal();
        assertEquals(1, guardadas.size());
        assertEquals(4, guardadas.getFirst().getCalificacion());
        assertEquals("Muy rico", guardadas.getFirst().getComentario());
        assertTrue(resenasRepo.existsByReserva_Id(compra.reservaId()));
    }

    @Test
    void registrarConComentarioEnBlancoLoGuardaVacio() {
        Long conBlanco = escenario.usuario();
        resenas.registrar(conBlanco, escenario.comprarYRecoger(conBlanco, oferta, local).reservaId(),
                new ResenaRequest(1, "   "));
        Long sinComentario = escenario.usuario();
        resenas.registrar(sinComentario, escenario.comprarYRecoger(sinComentario, oferta, local).reservaId(),
                new ResenaRequest(5, null));

        List<Resena> guardadas = resenasDelLocal();
        assertEquals(2, guardadas.size());
        assertTrue(guardadas.stream().allMatch(r -> r.getComentario() == null));
    }

    @Test
    void registrarResponde404SiLaCompraNoExisteOEsDeOtraPersona() {
        Long consumidor = escenario.usuario();
        CompraResponse compra = escenario.comprarYRecoger(consumidor, oferta, local);

        esperarEstado(HttpStatus.NOT_FOUND, () -> resenas.registrar(consumidor, Long.MAX_VALUE,
                new ResenaRequest(5, null)));
        esperarEstado(HttpStatus.NOT_FOUND, () -> resenas.registrar(escenario.usuario(), compra.reservaId(),
                new ResenaRequest(5, null)));
        assertTrue(resenasDelLocal().isEmpty());
    }

    @Test
    void soloSePuedeResenarDespuesDelRecojo() {
        Long consumidor = escenario.usuario();
        CompraResponse sinRecoger = escenario.comprar(consumidor, oferta);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> resenas.registrar(consumidor, sinRecoger.reservaId(), new ResenaRequest(5, null)));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("La compra aún no fue recogida", error.getReason());
        esperarEstado(HttpStatus.CONFLICT, () -> resenas.omitir(consumidor, sinRecoger.reservaId()));
    }

    @Test
    void unaSolaResenaPorCompraYPorLocal() {
        Long consumidor = escenario.usuario();
        CompraResponse primera = escenario.comprarYRecoger(consumidor, oferta, local);
        CompraResponse segunda = escenario.comprarYRecoger(consumidor, oferta, local);

        resenas.registrar(consumidor, primera.reservaId(), new ResenaRequest(5, "Excelente"));
        // Misma compra otra vez.
        esperarEstado(HttpStatus.CONFLICT, () -> resenas.registrar(consumidor, primera.reservaId(),
                new ResenaRequest(3, null)));
        // Otra compra del mismo local: ya reseñó este local.
        esperarEstado(HttpStatus.CONFLICT, () -> resenas.registrar(consumidor, segunda.reservaId(),
                new ResenaRequest(3, null)));
        // Tampoco queda una reseña pendiente que omitir.
        esperarEstado(HttpStatus.CONFLICT, () -> resenas.omitir(consumidor, primera.reservaId()));
        esperarEstado(HttpStatus.CONFLICT, () -> resenas.omitir(consumidor, segunda.reservaId()));
        assertEquals(1, resenasDelLocal().size());
    }

    @Test
    void omitirMarcaLaCompraYYaNoPermiteResenarlaNiOmitirlaOtraVez() {
        Long consumidor = escenario.usuario();
        CompraResponse compra = escenario.comprarYRecoger(consumidor, oferta, local);

        resenas.omitir(consumidor, compra.reservaId());

        assertTrue(reservasRepo.findById(compra.reservaId()).orElseThrow().isResenaOmitida());
        esperarEstado(HttpStatus.CONFLICT, () -> resenas.omitir(consumidor, compra.reservaId()));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> resenas.registrar(consumidor, compra.reservaId(), new ResenaRequest(4, null)));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Ya no se puede reseñar esta compra", error.getReason());
        assertTrue(resenasDelLocal().isEmpty());
    }

    @Test
    void omitirResponde404SiLaCompraNoExisteOEsDeOtraPersona() {
        Long consumidor = escenario.usuario();
        CompraResponse compra = escenario.comprarYRecoger(consumidor, oferta, local);

        esperarEstado(HttpStatus.NOT_FOUND, () -> resenas.omitir(consumidor, Long.MAX_VALUE));
        esperarEstado(HttpStatus.NOT_FOUND, () -> resenas.omitir(escenario.usuario(), compra.reservaId()));
        assertFalse(reservasRepo.findById(compra.reservaId()).orElseThrow().isResenaOmitida());
    }

    @Test
    void omitirNoProcedeSiYaResenoOtraCompraDelMismoLocal() {
        Long consumidor = escenario.usuario();
        CompraResponse resenada = escenario.comprarYRecoger(consumidor, oferta, local);
        CompraResponse otra = escenario.comprarYRecoger(consumidor, oferta, local);
        resenas.registrar(consumidor, resenada.reservaId(), new ResenaRequest(2, "Regular"));

        esperarEstado(HttpStatus.CONFLICT, () -> resenas.omitir(consumidor, otra.reservaId()));
        assertFalse(reservasRepo.findById(otra.reservaId()).orElseThrow().isResenaOmitida());
    }
}
