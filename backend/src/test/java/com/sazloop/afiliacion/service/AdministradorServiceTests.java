package com.sazloop.afiliacion.service;

import com.sazloop.afiliacion.domain.AdministradorPlataforma;
import com.sazloop.afiliacion.repository.AdministradorPlataformaRepository;
import com.sazloop.identidad.service.CodificadorContrasena;
import com.sazloop.shared.LimiteIntentosLogin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Inicio de sesión del personal interno, sesión actual y alta inicial del administrador. */
class AdministradorServiceTests {
    private AdministradorPlataformaRepository repositorio;
    private CodificadorContrasena codificador;
    private AdministradorService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(AdministradorPlataformaRepository.class);
        codificador = mock(CodificadorContrasena.class);
        servicio = new AdministradorService(repositorio, codificador, new LimiteIntentosLogin(3, 5));
    }

    @Test
    void iniciaSesionConCorreoYClaveCorrectos() {
        AdministradorPlataforma admin = admin(true);
        when(repositorio.findByEmailInstitucionalIgnoreCase("admin@sazloop.test")).thenReturn(Optional.of(admin));
        when(codificador.coincide("clave", "hash")).thenReturn(true);

        assertSame(admin, servicio.iniciar("  admin@sazloop.test  ", "clave"));
    }

    @Test
    void correoInexistenteClaveIncorrectaOCuentaInactivaSeRechazanIgual() {
        when(repositorio.findByEmailInstitucionalIgnoreCase(any())).thenReturn(Optional.empty());
        assertDenegado(() -> servicio.iniciar("nadie@sazloop.test", "clave"));
        verify(codificador).rellenar("clave");

        AdministradorPlataforma activo = admin(true);
        when(repositorio.findByEmailInstitucionalIgnoreCase("activo@sazloop.test")).thenReturn(Optional.of(activo));
        when(codificador.coincide("mala", "hash")).thenReturn(false);
        assertDenegado(() -> servicio.iniciar("activo@sazloop.test", "mala"));

        AdministradorPlataforma inactivo = admin(false);
        when(repositorio.findByEmailInstitucionalIgnoreCase("baja@sazloop.test")).thenReturn(Optional.of(inactivo));
        when(codificador.coincide("clave", "hash")).thenReturn(true);
        assertDenegado(() -> servicio.iniciar("baja@sazloop.test", "clave"));
    }

    @Test
    void trasVariosFallosElCorreoQuedaBloqueado() {
        when(repositorio.findByEmailInstitucionalIgnoreCase(any())).thenReturn(Optional.empty());
        for (int i = 0; i < 3; i++) assertDenegado(() -> servicio.iniciar("bloqueo@sazloop.test", "x"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.iniciar("bloqueo@sazloop.test", "x"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void obtenerSoloDevuelveAdministradoresActivos() {
        AdministradorPlataforma activo = admin(true);
        when(repositorio.findById(1L)).thenReturn(Optional.of(activo));
        when(repositorio.findById(2L)).thenReturn(Optional.of(admin(false)));
        when(repositorio.findById(3L)).thenReturn(Optional.empty());

        assertSame(activo, servicio.obtener(1L));
        assertDenegado(() -> servicio.obtener(2L));
        assertDenegado(() -> servicio.obtener(3L));
    }

    @Test
    void elAltaInicialCreaElAdministradorSoloSiFaltaYHayDatos() throws Exception {
        servicio.provisionarAdministrador("", "clave").run();
        servicio.provisionarAdministrador("admin@sazloop.test", "  ").run();
        verifyNoInteractions(repositorio);

        when(repositorio.findByEmailInstitucionalIgnoreCase("existe@sazloop.test"))
                .thenReturn(Optional.of(admin(true)));
        servicio.provisionarAdministrador("existe@sazloop.test", "clave").run();
        verify(repositorio, never()).save(any());

        when(repositorio.findByEmailInstitucionalIgnoreCase("nuevo@sazloop.test")).thenReturn(Optional.empty());
        when(codificador.codificar("clave-inicial")).thenReturn("hash-inicial");
        CommandLineRunner alta = servicio.provisionarAdministrador("  Nuevo@Sazloop.TEST ", "clave-inicial");
        alta.run();
        ArgumentCaptor<AdministradorPlataforma> guardado = ArgumentCaptor.forClass(AdministradorPlataforma.class);
        verify(repositorio).save(guardado.capture());
        assertEquals("nuevo@sazloop.test", guardado.getValue().getEmailInstitucional());
        assertEquals("hash-inicial", guardado.getValue().getPasswordHash());
        assertEquals("Administrador", guardado.getValue().getNombre());
    }

    @Test
    void laSesionActualExigeUnIdentificadorDeAdministrador() {
        AdministradorService delegado = mock(AdministradorService.class);
        AdministradorSesionActual sesionActual = new AdministradorSesionActual(delegado);

        assertNoAutorizado(sesionActual, new MockHttpServletRequest());
        MockHttpServletRequest conTexto = new MockHttpServletRequest();
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("administradorId", "7");
        conTexto.setSession(sesion);
        assertNoAutorizado(sesionActual, conTexto);

        AdministradorPlataforma admin = admin(true);
        when(delegado.obtener(7L)).thenReturn(admin);
        MockHttpServletRequest valida = new MockHttpServletRequest();
        MockHttpSession buena = new MockHttpSession();
        buena.setAttribute("administradorId", 7L);
        valida.setSession(buena);
        assertSame(admin, sesionActual.obtener(valida));
    }

    private static void assertNoAutorizado(AdministradorSesionActual sesionActual, MockHttpServletRequest peticion) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> sesionActual.obtener(peticion));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Inicia sesión como administrador", ex.getReason());
    }

    private static void assertDenegado(org.junit.jupiter.api.function.Executable accion) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, accion);
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Acceso administrativo inválido", ex.getReason());
    }

    private static AdministradorPlataforma admin(boolean activo) {
        return AdministradorPlataforma.builder().id(1L).nombre("Admin").emailInstitucional("admin@sazloop.test")
                .passwordHash("hash").activo(activo).build();
    }
}
