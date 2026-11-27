package com.sazloop.afiliacion.service;

import com.sazloop.afiliacion.domain.AdministradorPlataforma;
import com.sazloop.afiliacion.repository.AdministradorPlataformaRepository;
import com.sazloop.identidad.service.CodificadorContrasena;
import com.sazloop.shared.LimiteIntentosLogin;
import com.sazloop.shared.SesionesPorRol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Inicio de sesión del administrador y creación de su cuenta al arrancar, con la base H2 real. */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class AdministradorAccesoTests {
    private static final String CLAVE = "ClaveAdmin1";

    @Autowired AdministradorService servicio;
    @Autowired AdministradorSesionActual sesionActual;
    @Autowired AdministradorPlataformaRepository administradores;
    @Autowired CodificadorContrasena codificador;

    @Test
    void iniciaConElCorreoSinImportarMayusculasNiEspacios() {
        AdministradorPlataforma admin = admin(true);

        var resultado = servicio.iniciar("  " + admin.getEmailInstitucional().toUpperCase() + " ", CLAVE);

        assertEquals(admin.getId(), resultado.getId());
        assertEquals(admin.getId(), servicio.obtener(admin.getId()).getId());
    }

    @Test
    void rechazaCorreoInexistenteClaveIncorrectaYCuentaInactiva() {
        AdministradorPlataforma activo = admin(true);
        AdministradorPlataforma inactivo = admin(false);

        assertStatus(HttpStatus.UNAUTHORIZED, () -> servicio.iniciar(UUID.randomUUID() + "@example.test", CLAVE));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> servicio.iniciar(activo.getEmailInstitucional(), "mala"));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> servicio.iniciar(inactivo.getEmailInstitucional(), CLAVE));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> servicio.obtener(inactivo.getId()));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> servicio.obtener(-1L));
    }

    @Test
    void trasCincoFallosQuedaBloqueadoCon429() {
        AdministradorPlataforma admin = admin(true);
        for (int i = 0; i < 5; i++)
            assertStatus(HttpStatus.UNAUTHORIZED, () -> servicio.iniciar(admin.getEmailInstitucional(), "mala"));

        assertStatus(HttpStatus.TOO_MANY_REQUESTS, () -> servicio.iniciar(admin.getEmailInstitucional(), CLAVE));
    }

    @Test
    void laSesionActualExigeUnIdNumericoDeUnAdministradorActivo() {
        AdministradorPlataforma activo = admin(true);
        AdministradorPlataforma inactivo = admin(false);

        assertStatus(HttpStatus.UNAUTHORIZED, () -> sesionActual.obtener(new MockHttpServletRequest()));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> sesionActual.obtener(peticion("texto")));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> sesionActual.obtener(peticion(inactivo.getId())));
        assertEquals(activo.getId(), sesionActual.obtener(peticion(activo.getId())).getId());
    }

    @Test
    void alArrancarCreaLaCuentaConfiguradaUnaSolaVez() throws Exception {
        AdministradorService nuevo = new AdministradorService(administradores, codificador, new LimiteIntentosLogin(5, 5));
        String correo = "Admin." + UUID.randomUUID() + "@Example.TEST";

        nuevo.provisionarAdministrador(" " + correo + " ", CLAVE).run();
        nuevo.provisionarAdministrador(correo, "OtraClave").run();

        var creado = administradores.findByEmailInstitucionalIgnoreCase(correo).orElseThrow();
        assertEquals(correo.toLowerCase(), creado.getEmailInstitucional());
        assertEquals("Administrador", creado.getNombre());
        // La segunda ejecución no reemplaza la contraseña existente.
        assertTrue(codificador.coincide(CLAVE, creado.getPasswordHash()));
    }

    @Test
    void sinCorreoOContrasenaConfiguradosNoCreaNada() throws Exception {
        AdministradorService nuevo = new AdministradorService(administradores, codificador, new LimiteIntentosLogin(5, 5));
        long antes = administradores.count();

        nuevo.provisionarAdministrador("", CLAVE).run();
        nuevo.provisionarAdministrador("  ", CLAVE).run();
        nuevo.provisionarAdministrador(UUID.randomUUID() + "@example.test", " ").run();

        assertEquals(antes, administradores.count());
    }

    private AdministradorPlataforma admin(boolean activo) {
        return administradores.save(AdministradorPlataforma.builder().nombre("Revisor")
                .emailInstitucional(UUID.randomUUID() + "@example.test")
                .passwordHash(codificador.codificar(CLAVE)).activo(activo).build());
    }

    private static MockHttpServletRequest peticion(Object valor) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(SesionesPorRol.ADMINISTRADOR, valor);
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setSession(sesion);
        return peticion;
    }

    private static void assertStatus(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado, error.getStatusCode());
    }
}
