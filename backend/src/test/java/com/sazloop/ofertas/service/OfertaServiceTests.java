package com.sazloop.ofertas.service;

import com.sazloop.experiencia.dto.ResenaRequest;
import com.sazloop.experiencia.service.ResenaService;
import com.sazloop.identidad.domain.CuentaEstablecimiento;
import com.sazloop.identidad.domain.Establecimiento;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import com.sazloop.ofertas.EscenarioOfertas;
import com.sazloop.ofertas.EscenarioOfertas.LocalPrueba;
import com.sazloop.ofertas.domain.OfertaPack;
import com.sazloop.ofertas.domain.PackSorpresa;
import com.sazloop.ofertas.dto.OfertaRequest;
import com.sazloop.ofertas.dto.OfertaResponse;
import com.sazloop.ofertas.dto.PackRequest;
import com.sazloop.ofertas.repository.AlergenoRepository;
import com.sazloop.ofertas.repository.OfertaPackRepository;
import com.sazloop.ofertas.repository.PackSorpresaRepository;
import com.sazloop.shared.Tiempo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.sazloop.ofertas.EscenarioOfertas.unico;
import static com.sazloop.ofertas.EscenarioOfertas.variante;
import static com.sazloop.ofertas.domain.VariantePack.TamanioVariante.*;
import static org.junit.jupiter.api.Assertions.*;

/** Publicación de ofertas, búsqueda pública con sus filtros y ficha pública, con H2 real. */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class OfertaServiceTests {
    @Autowired ApplicationContext contexto;
    @Autowired OfertaService ofertas;
    @Autowired OfertaPackRepository ofertasRepo;
    @Autowired PackSorpresaRepository packsRepo;
    @Autowired AlergenoRepository alergenosRepo;
    @Autowired ResenaService resenas;
    @Autowired TransactionTemplate transacciones;

    EscenarioOfertas escenario;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioOfertas(contexto);
    }

    private static void esperarEstado(HttpStatus estado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(estado, error.getStatusCode());
    }

    private OfertaRequest oferta(Long packId, LocalDateTime inicio, LocalDateTime fin,
            OfertaRequest.VarianteRequest... variantes) {
        return new OfertaRequest(packId, inicio, fin, List.of(variantes));
    }

    // ---------- Búsqueda pública ----------

    @Test
    void buscarFiltraPorDistritoYDireccionSinImportarMayusculas() {
        String distrito = unico("Distrito");
        LocalPrueba local = escenario.localActivo(distrito, "Avenida Larco 123");
        OfertaResponse publicada = escenario.publicar(local, 2);

        List<OfertaResponse> porDistrito = ofertas.buscar(distrito.toUpperCase(), null, null, null);
        assertEquals(List.of(publicada.id()), porDistrito.stream().map(OfertaResponse::id).toList());
        assertNull(porDistrito.getFirst().distanciaKm());
        assertEquals(1, ofertas.buscar(distrito, "  larco ", null, null).size());
        assertEquals(1, ofertas.buscar("  " + distrito + "  ", "", null, null).size());
        assertTrue(ofertas.buscar(distrito, "Javier Prado", null, null).isEmpty());
    }

    @Test
    void buscarTrataLosComodinesDelUsuarioComoTextoLiteral() {
        String distrito = unico("Comodin");
        LocalPrueba local = escenario.localActivo(distrito, "Jr. 50% Ofertas_Uno!");
        escenario.publicar(local, 1);

        // «%» y «_» no actúan como comodines de LIKE: solo coinciden con su propio carácter.
        assertTrue(ofertas.buscar(distrito, "Jr.%Ofertas", null, null).isEmpty());
        assertTrue(ofertas.buscar(distrito, "Ofertas Uno", null, null).isEmpty());
        assertEquals(1, ofertas.buscar(distrito, "50% ofertas_uno!", null, null).size());
        assertEquals(1, ofertas.buscar(distrito, "%", null, null).size());
        assertTrue(ofertas.buscar(distrito, "!!", null, null).isEmpty());
    }

    @Test
    void buscarPorPrecioMaximoConsideraSoloVariantesConStock() {
        String distrito = unico("Precio");
        LocalPrueba barato = escenario.localActivo(distrito, "Calle Barata");
        LocalPrueba caro = escenario.localActivo(distrito, "Calle Cara");
        LocalPrueba agotado = escenario.localActivo(distrito, "Calle Agotada");
        OfertaResponse ofertaBarata = escenario.publicar(barato,
                variante(PEQUENO, "20.00", "8.00", 1), variante(GRANDE, "40.00", "25.00", 1));
        escenario.publicar(caro, variante(MEDIANO, "50.00", "30.00", 3));
        OfertaResponse sinStock = escenario.publicar(agotado, variante(PEQUENO, "20.00", "5.00", 1));
        // Se agota directamente en la base para no depender del flujo de compra.
        transacciones.executeWithoutResult(estado -> ofertasRepo.findById(sinStock.id()).orElseThrow()
                .getVariantes().getFirst().setStockDisponible(0));

        assertEquals(List.of(ofertaBarata.id()), ofertas.buscar(distrito, null, new BigDecimal("10.00"), null)
                .stream().map(OfertaResponse::id).toList());
        assertEquals(2, ofertas.buscar(distrito, null, new BigDecimal("30.00"), null).size());
        // Sin filtro de precio, la oferta agotada tampoco aparece.
        assertEquals(2, ofertas.buscar(distrito, null, null, null).size());
    }

    @Test
    void buscarRechazaFiltrosDePrecioYCalificacionInvalidos() {
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(null, null, BigDecimal.ZERO, null));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(null, null, new BigDecimal("-1"), null));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(null, null, null, -0.5));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(null, null, null, 5.1));
    }

    @Test
    void buscarRechazaUbicacionesIncompletasOFueraDeRango() {
        String d = unico("Ubicacion");
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(d, null, null, null, -12.0, null));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(d, null, null, null, null, -77.0));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(d, null, null, null, Double.NaN, -77.0));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(d, null, null, null, -12.0, Double.POSITIVE_INFINITY));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(d, null, null, null, -90.01, -77.0));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(d, null, null, null, 90.01, -77.0));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(d, null, null, null, -12.0, -180.01));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.buscar(d, null, null, null, -12.0, 180.01));
        // Los extremos exactos sí son válidos (y no hay ofertas en ese distrito inventado).
        assertTrue(ofertas.buscar(d, null, null, null, 90.0, 180.0).isEmpty());
        assertTrue(ofertas.buscar(d, null, null, null, -90.0, -180.0).isEmpty());
    }

    @Test
    void cercaDeMiOrdenaPorDistanciaYDejaAlFinalLosLocalesSinCoordenadas() {
        String distrito = unico("Cerca");
        LocalPrueba sinCoordenadas = escenario.localActivo(distrito, "Calle Sin Mapa");
        LocalPrueba lejano = escenario.localActivo(distrito, "Calle Lejana", "-12.200000", "-77.000000");
        LocalPrueba cercano = escenario.localActivo(distrito, "Calle Cercana", "-12.050000", "-77.040000");
        OfertaResponse ofertaSin = escenario.publicar(sinCoordenadas, 1);
        OfertaResponse ofertaLejana = escenario.publicar(lejano, 1);
        OfertaResponse ofertaCercana = escenario.publicar(cercano, 1);

        List<OfertaResponse> resultado = ofertas.buscar(distrito, null, null, null, -12.05, -77.04);
        assertEquals(List.of(ofertaCercana.id(), ofertaLejana.id(), ofertaSin.id()),
                resultado.stream().map(OfertaResponse::id).toList());
        assertEquals(0.0, resultado.get(0).distanciaKm());
        assertTrue(resultado.get(1).distanciaKm() > 16 && resultado.get(1).distanciaKm() < 18,
                "unos 17 km entre los dos puntos, pero fue " + resultado.get(1).distanciaKm());
        assertNull(resultado.get(2).distanciaKm());

        // Sin ubicación se conserva el orden por publicación (la más reciente primero) y no hay distancias.
        List<OfertaResponse> sinUbicacion = ofertas.buscar(distrito, null, null, null);
        assertEquals(List.of(ofertaCercana.id(), ofertaLejana.id(), ofertaSin.id()),
                sinUbicacion.stream().map(OfertaResponse::id).toList());
        assertTrue(sinUbicacion.stream().allMatch(o -> o.distanciaKm() == null));
    }

    @Test
    void buscarPorCalificacionMinimaExcluyeLocalesSinResenasOConPromedioMenor() {
        String distrito = unico("Calificado");
        LocalPrueba bueno = escenario.localActivo(distrito, "Calle Buena");
        LocalPrueba regular = escenario.localActivo(distrito, "Calle Regular");
        LocalPrueba triple = escenario.localActivo(distrito, "Calle Triple");
        LocalPrueba nuevo = escenario.localActivo(distrito, "Calle Nueva");
        OfertaResponse ofertaBuena = escenario.publicar(bueno, 5);
        OfertaResponse ofertaRegular = escenario.publicar(regular, 5);
        OfertaResponse ofertaTriple = escenario.publicar(triple, 5);
        escenario.publicar(nuevo, 5);
        resenar(bueno, ofertaBuena, 5);
        resenar(bueno, ofertaBuena, 4);
        resenar(regular, ofertaRegular, 2);
        resenar(triple, ofertaTriple, 5);
        resenar(triple, ofertaTriple, 5);
        resenar(triple, ofertaTriple, 4);

        List<OfertaResponse> todas = ofertas.buscar(distrito, null, null, null);
        assertEquals(4, todas.size());
        assertEquals(4.5, todas.stream().filter(o -> o.id().equals(ofertaBuena.id())).findFirst().orElseThrow().calificacion());
        assertEquals(1, todas.stream().filter(o -> o.calificacion() == null).count());

        assertEquals(List.of(ofertaTriple.id(), ofertaBuena.id()), ids(ofertas.buscar(distrito, null, null, 4.0)));
        // El valor exacto del promedio también cumple (4,5 >= 4,5).
        assertEquals(List.of(ofertaTriple.id(), ofertaBuena.id()), ids(ofertas.buscar(distrito, null, null, 4.5)));
        assertEquals(List.of(ofertaTriple.id()), ids(ofertas.buscar(distrito, null, null, 4.6)));
        // 14/3 = 4,6666…: la base la deja pasar por su margen de 0,001, pero el filtro exacto la descarta.
        assertTrue(ofertas.buscar(distrito, null, null, 4.6667).isEmpty());
        assertTrue(ofertas.buscar(distrito, null, null, 5.0).isEmpty());
        // Con mínimo 0 quedan solo los locales que tienen alguna reseña.
        assertEquals(3, ofertas.buscar(distrito, null, null, 0.0).size());
    }

    private static List<Long> ids(List<OfertaResponse> lista) {
        return lista.stream().map(OfertaResponse::id).toList();
    }

    private void resenar(LocalPrueba local, OfertaResponse oferta, int estrellas) {
        Long consumidor = escenario.usuario();
        var compra = escenario.comprarYRecoger(consumidor, oferta, local);
        resenas.registrar(consumidor, compra.reservaId(), new ResenaRequest(estrellas, "Reseña " + estrellas));
    }

    @Test
    void buscarNoMuestraOfertasVencidasNiDeLocalesSuspendidos() {
        String distrito = unico("Oculto");
        LocalPrueba vencido = escenario.localActivo(distrito, "Calle Vencida");
        LocalPrueba suspendido = escenario.localActivo(distrito, "Calle Suspendida");
        OfertaResponse ofertaVencida = escenario.publicar(vencido, 1);
        escenario.publicar(suspendido, 1);
        assertEquals(2, ofertas.buscar(distrito, null, null, null).size());

        OfertaPack entidad = ofertasRepo.findById(ofertaVencida.id()).orElseThrow();
        entidad.setFinRecojo(Tiempo.ahora().minusSeconds(1));
        ofertasRepo.saveAndFlush(entidad);
        escenario.cambiarEstado(suspendido.localId(), EstadoEstablecimiento.SUSPENDIDO);

        assertTrue(ofertas.buscar(distrito, null, null, null).isEmpty());
    }

    // ---------- Ficha pública ----------

    @Test
    void consultarDevuelveLaFichaConAhorroYAdvertenciasOrdenadas() {
        LocalPrueba local = escenario.localActivo(unico("Ficha"), "Calle Ficha");
        String nuez = unico("Nuez");
        String sinLactosa = unico("Sin lactosa");
        String vegetariano = unico("Vegetariano");
        Long pack = ofertas.crearPack(local.cuenta(), new PackRequest(" Pack ficha ", " Variado ", " Pan ",
                List.of(nuez, " " + nuez.toUpperCase() + " ", "Trigo"), List.of(vegetariano, sinLactosa)));
        LocalDateTime inicio = Tiempo.ahora();
        OfertaResponse publicada = ofertas.publicar(local.cuenta(), oferta(pack, inicio, inicio.plusHours(1),
                variante(PEQUENO, "20.00", "12.50", 2), variante(GRANDE, "35.00", "20.00", 1)));

        OfertaResponse ficha = ofertas.consultar(publicada.id());
        assertEquals("Pack ficha", ficha.pack());
        assertEquals("Variado", ficha.descripcion());
        // El alérgeno repetido con otras mayúsculas se guarda una sola vez.
        assertEquals(List.of(nuez, "Trigo"), ficha.alergenos());
        assertEquals(List.of(sinLactosa, vegetariano), ficha.restricciones());
        assertNull(ficha.calificacion());
        assertEquals(OfertaPack.EstadoOferta.ACTIVA, ficha.estado());
        assertEquals(ficha.finRecojo(), ficha.limiteDefinitivo());
        var pequeno = ficha.variantes().stream().filter(v -> v.tamanio() == PEQUENO).findFirst().orElseThrow();
        assertEquals(new BigDecimal("7.50"), pequeno.ahorro());
        assertEquals(2, pequeno.stockDisponible());
    }

    @Test
    void consultarResponde404SiLaOfertaNoExisteNoEstaActivaVencioOElLocalNoEstaActivo() {
        esperarEstado(HttpStatus.NOT_FOUND, () -> ofertas.consultar(Long.MAX_VALUE));

        LocalPrueba finalizada = escenario.localActivo(unico("Fin"), "Calle Fin");
        OfertaResponse o1 = escenario.publicar(finalizada, 1);
        OfertaPack e1 = ofertasRepo.findById(o1.id()).orElseThrow();
        e1.setEstado(OfertaPack.EstadoOferta.FINALIZADA);
        ofertasRepo.saveAndFlush(e1);
        esperarEstado(HttpStatus.NOT_FOUND, () -> ofertas.consultar(o1.id()));

        LocalPrueba vencida = escenario.localActivo(unico("Vence"), "Calle Vence");
        OfertaResponse o2 = escenario.publicar(vencida, 1);
        OfertaPack e2 = ofertasRepo.findById(o2.id()).orElseThrow();
        e2.setFinRecojo(Tiempo.ahora().minusMinutes(1));
        ofertasRepo.saveAndFlush(e2);
        esperarEstado(HttpStatus.NOT_FOUND, () -> ofertas.consultar(o2.id()));

        LocalPrueba inactivo = escenario.localActivo(unico("Inactivo"), "Calle Inactiva");
        OfertaResponse o3 = escenario.publicar(inactivo, 1);
        assertEquals(o3.id(), ofertas.consultar(o3.id()).id());
        escenario.cambiarEstado(inactivo.localId(), EstadoEstablecimiento.INACTIVO);
        esperarEstado(HttpStatus.NOT_FOUND, () -> ofertas.consultar(o3.id()));
    }

    // ---------- Packs ----------

    @Test
    void crearPackReutilizaAdvertenciasExistentesYListarOcultaPacksInactivos() {
        LocalPrueba local = escenario.localActivo(unico("Packs"), "Calle Packs");
        String alergeno = unico("Alergeno");
        Long primero = ofertas.crearPack(local.cuenta(), new PackRequest("Primero", "D", "P",
                List.of(alergeno), null));
        long antes = alergenosRepo.count();
        Long segundo = ofertas.crearPack(local.cuenta(), new PackRequest("Segundo", "D", "P",
                List.of(alergeno.toUpperCase()), List.of()));
        assertEquals(antes, alergenosRepo.count(), "el alérgeno ya existía y se reutiliza");

        PackSorpresa inactivo = packsRepo.findById(primero).orElseThrow();
        inactivo.setActivo(false);
        packsRepo.saveAndFlush(inactivo);
        assertEquals(List.of(new OfertaService.PackDisponible(segundo, "Segundo")),
                ofertas.listarPacks(local.cuenta()));
    }

    @Test
    void crearPackRechazaAdvertenciasVaciasOMuyLargas() {
        LocalPrueba local = escenario.localActivo(unico("Advertencia"), "Calle Advertencia");
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.crearPack(local.cuenta(),
                new PackRequest("P", "D", "X", List.of("   "), null)));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.crearPack(local.cuenta(),
                new PackRequest("P", "D", "X", List.of("a".repeat(81)), null)));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.crearPack(local.cuenta(),
                new PackRequest("P", "D", "X", null, List.of("b".repeat(101)))));
        // Los límites exactos sí se aceptan.
        assertNotNull(ofertas.crearPack(local.cuenta(), new PackRequest("P", "D", "X",
                List.of(unico("a").repeat(6).substring(0, 80)), List.of(unico("b").repeat(8).substring(0, 100)))));
    }

    @Test
    void crearPackYPublicarFallanCon404SiElLocalNoExiste() {
        CuentaEstablecimiento fantasma = CuentaEstablecimiento.builder()
                .establecimiento(Establecimiento.builder().id(Long.MAX_VALUE).build()).build();
        esperarEstado(HttpStatus.NOT_FOUND, () -> ofertas.crearPack(fantasma, EscenarioOfertas.pack("X")));
        esperarEstado(HttpStatus.NOT_FOUND, () -> ofertas.publicar(fantasma, oferta(1L, Tiempo.ahora(),
                Tiempo.ahora().plusHours(1), variante(PEQUENO, "2.00", "1.00", 1))));
    }

    // ---------- Reglas de publicación ----------

    @Test
    void publicarExigeLocalActivo() {
        LocalPrueba local = escenario.localActivo(unico("Estado"), "Calle Estado");
        Long pack = ofertas.crearPack(local.cuenta(), EscenarioOfertas.pack("Pack estado"));
        LocalDateTime inicio = Tiempo.ahora().plusMinutes(5);
        OfertaRequest pedido = oferta(pack, inicio, inicio.plusHours(1), variante(PEQUENO, "10.00", "5.00", 1));

        escenario.cambiarEstado(local.localId(), EstadoEstablecimiento.SUSPENDIDO);
        ResponseStatusException suspendido = assertThrows(ResponseStatusException.class,
                () -> ofertas.publicar(local.cuenta(), pedido));
        assertEquals(HttpStatus.CONFLICT, suspendido.getStatusCode());
        assertTrue(suspendido.getReason().contains("suspendido"));

        escenario.cambiarEstado(local.localId(), EstadoEstablecimiento.PENDIENTE_APROBACION);
        ResponseStatusException pendiente = assertThrows(ResponseStatusException.class,
                () -> ofertas.publicar(local.cuenta(), pedido));
        assertEquals(HttpStatus.CONFLICT, pendiente.getStatusCode());
        assertEquals("Local no aprobado", pendiente.getReason());

        escenario.cambiarEstado(local.localId(), EstadoEstablecimiento.ACTIVO);
        assertEquals(OfertaPack.EstadoOferta.ACTIVA, ofertas.publicar(local.cuenta(), pedido).estado());
    }

    @Test
    void publicarSoloAceptaPacksActivosDelPropioLocal() {
        LocalPrueba local = escenario.localActivo(unico("Propio"), "Calle Propia");
        LocalPrueba otro = escenario.localActivo(unico("Ajeno"), "Calle Ajena");
        Long packAjeno = ofertas.crearPack(otro.cuenta(), EscenarioOfertas.pack("Ajeno"));
        Long packInactivo = ofertas.crearPack(local.cuenta(), EscenarioOfertas.pack("Inactivo"));
        PackSorpresa entidad = packsRepo.findById(packInactivo).orElseThrow();
        entidad.setActivo(false);
        packsRepo.saveAndFlush(entidad);
        LocalDateTime inicio = Tiempo.ahora().plusMinutes(1);

        esperarEstado(HttpStatus.NOT_FOUND, () -> ofertas.publicar(local.cuenta(),
                oferta(packAjeno, inicio, inicio.plusHours(1), variante(PEQUENO, "10.00", "5.00", 1))));
        esperarEstado(HttpStatus.NOT_FOUND, () -> ofertas.publicar(local.cuenta(),
                oferta(packInactivo, inicio, inicio.plusHours(1), variante(PEQUENO, "10.00", "5.00", 1))));
        esperarEstado(HttpStatus.NOT_FOUND, () -> ofertas.publicar(local.cuenta(),
                oferta(Long.MAX_VALUE, inicio, inicio.plusHours(1), variante(PEQUENO, "10.00", "5.00", 1))));
    }

    @Test
    void publicarValidaElHorarioDeRecojo() {
        LocalPrueba local = escenario.localActivo(unico("Horario"), "Calle Horario");
        Long pack = ofertas.crearPack(local.cuenta(), EscenarioOfertas.pack("Horario"));
        LocalDateTime ahora = Tiempo.ahora();
        var v = variante(PEQUENO, "10.00", "5.00", 1);

        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.publicar(local.cuenta(),
                oferta(pack, ahora.minusMinutes(10), ahora.plusHours(1), v)));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.publicar(local.cuenta(),
                oferta(pack, ahora.plusHours(1), ahora.plusHours(1), v)));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.publicar(local.cuenta(),
                oferta(pack, ahora.plusHours(2), ahora.plusHours(1), v)));
        // Un inicio de hace dos minutos se tolera por la diferencia de reloj del navegador.
        assertNotNull(ofertas.publicar(local.cuenta(), oferta(pack, ahora.minusMinutes(2), ahora.plusHours(1), v)).id());
    }

    @Test
    void publicarValidaVariantesRepetidasStockYPrecios() {
        LocalPrueba local = escenario.localActivo(unico("Variantes"), "Calle Variantes");
        Long pack = ofertas.crearPack(local.cuenta(), EscenarioOfertas.pack("Variantes"));
        LocalDateTime inicio = Tiempo.ahora().plusMinutes(1);
        LocalDateTime fin = inicio.plusHours(1);

        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.publicar(local.cuenta(), oferta(pack, inicio, fin,
                variante(PEQUENO, "10.00", "5.00", 1), variante(PEQUENO, "12.00", "6.00", 1))));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.publicar(local.cuenta(), oferta(pack, inicio, fin,
                variante(MEDIANO, "10.00", "5.00", 0))));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.publicar(local.cuenta(), oferta(pack, inicio, fin,
                variante(MEDIANO, "10.00", "10.00", 1))));
        esperarEstado(HttpStatus.BAD_REQUEST, () -> ofertas.publicar(local.cuenta(), oferta(pack, inicio, fin,
                variante(MEDIANO, "10.00", "11.00", 1))));
        // Ninguno de los intentos fallidos dejó una oferta activa.
        assertTrue(ofertas.vigente(local.cuenta()).isEmpty());

        OfertaResponse valida = ofertas.publicar(local.cuenta(), oferta(pack, inicio, fin,
                variante(PEQUENO, "10.00", "5.00", 1), variante(MEDIANO, "15.00", "7.00", 2),
                variante(GRANDE, "20.00", "9.00", 3)));
        assertEquals(3, valida.variantes().size());
    }

    @Test
    void soloUnaOfertaActivaPorLocalYLaVencidaSeFinalizaAlPublicarOtra() {
        LocalPrueba local = escenario.localActivo(unico("Unica"), "Calle Unica");
        OfertaResponse primera = escenario.publicar(local, 1);
        assertEquals(primera.id(), ofertas.vigente(local.cuenta()).orElseThrow().id());

        Long pack = ofertas.crearPack(local.cuenta(), EscenarioOfertas.pack("Segundo"));
        LocalDateTime inicio = Tiempo.ahora().plusMinutes(1);
        OfertaRequest segunda = oferta(pack, inicio, inicio.plusHours(1), variante(GRANDE, "30.00", "15.00", 2));
        ResponseStatusException conflicto = assertThrows(ResponseStatusException.class,
                () -> ofertas.publicar(local.cuenta(), segunda));
        assertEquals(HttpStatus.CONFLICT, conflicto.getStatusCode());

        OfertaPack vencida = ofertasRepo.findById(primera.id()).orElseThrow();
        vencida.setFinRecojo(Tiempo.ahora().minusSeconds(1));
        ofertasRepo.saveAndFlush(vencida);
        // La vencida sigue ACTIVA en la base pero ya no es la vigente.
        assertTrue(ofertas.vigente(local.cuenta()).isEmpty());

        OfertaResponse nueva = ofertas.publicar(local.cuenta(), segunda);
        assertEquals(nueva.id(), ofertas.vigente(local.cuenta()).orElseThrow().id());
        assertEquals(OfertaPack.EstadoOferta.FINALIZADA, ofertasRepo.findById(primera.id()).orElseThrow().getEstado());
    }
}
