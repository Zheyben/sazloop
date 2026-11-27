package com.sazloop.shared;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias de las sesiones por rol que comparten una misma cookie. */
class SesionesPorRolTests {

    @Test
    void abrirCreaLaSesionGuardaElRolYRenuevaElIdentificador() {
        MockHttpSession sesion = new MockHttpSession();
        String idAnterior = sesion.getId();
        MockHttpServletRequest peticion = peticion(sesion);

        SesionesPorRol.abrir(peticion, SesionesPorRol.PERSONA, 15L);

        assertEquals(15L, sesion.getAttribute(SesionesPorRol.PERSONA));
        assertNotEquals(idAnterior, sesion.getId(), "debe cambiar el id para evitar la fijación de sesión");
    }

    @Test
    void abrirSinSesionPreviaLaCrea() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();

        SesionesPorRol.abrir(peticion, SesionesPorRol.LOCAL, 3L);

        assertNotNull(peticion.getSession(false));
        assertEquals(3L, peticion.getSession(false).getAttribute(SesionesPorRol.LOCAL));
    }

    @Test
    void cerrarSinSesionNoHaceNada() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();

        assertDoesNotThrow(() -> SesionesPorRol.cerrar(peticion, SesionesPorRol.ADMINISTRADOR));
        assertNull(peticion.getSession(false));
    }

    @Test
    void cerrarUnRolConservaLosDemasYLaUltimaInvalidaLaSesion() {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(SesionesPorRol.PERSONA, 1L);
        sesion.setAttribute(SesionesPorRol.LOCAL, 2L);
        sesion.setAttribute(SesionesPorRol.ADMINISTRADOR, 3L);

        SesionesPorRol.cerrar(peticion(sesion), SesionesPorRol.LOCAL);
        assertNull(sesion.getAttribute(SesionesPorRol.LOCAL));
        assertFalse(sesion.isInvalid());

        SesionesPorRol.cerrar(peticion(sesion), SesionesPorRol.PERSONA);
        assertEquals(3L, sesion.getAttribute(SesionesPorRol.ADMINISTRADOR));
        assertFalse(sesion.isInvalid());

        SesionesPorRol.cerrar(peticion(sesion), SesionesPorRol.ADMINISTRADOR);
        assertTrue(sesion.isInvalid());
    }

    private static MockHttpServletRequest peticion(MockHttpSession sesion) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setSession(sesion);
        return peticion;
    }
}
