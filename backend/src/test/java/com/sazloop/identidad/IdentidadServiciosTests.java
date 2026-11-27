package com.sazloop.identidad;

import com.sazloop.identidad.domain.CuentaEstablecimiento;
import com.sazloop.identidad.domain.Establecimiento;
import com.sazloop.identidad.domain.Establecimiento.EstadoEstablecimiento;
import com.sazloop.identidad.domain.Usuario;
import com.sazloop.identidad.dto.CambioContrasenaRequest;
import com.sazloop.identidad.dto.CuentaLocalRequest;
import com.sazloop.identidad.dto.DireccionRequest;
import com.sazloop.identidad.dto.EmpresaRequest;
import com.sazloop.identidad.dto.EstablecimientoRequest;
import com.sazloop.identidad.dto.InicioSesionRequest;
import com.sazloop.identidad.dto.PerfilRequest;
import com.sazloop.identidad.dto.RegistroUsuarioRequest;
import com.sazloop.identidad.dto.UbicacionRequest;
import com.sazloop.identidad.repository.CuentaEstablecimientoRepository;
import com.sazloop.identidad.repository.EstablecimientoRepository;
import com.sazloop.identidad.repository.UsuarioRepository;
import com.sazloop.identidad.service.CodificadorContrasena;
import com.sazloop.identidad.service.CuentaLocalActual;
import com.sazloop.identidad.service.CuentaLocalService;
import com.sazloop.identidad.service.EmpresaService;
import com.sazloop.identidad.service.PerfilService;
import com.sazloop.identidad.service.RegistroUsuarioService;
import com.sazloop.identidad.service.SesionUsuarioService;
import com.sazloop.identidad.service.UsuarioSesionActual;
import com.sazloop.shared.SesionesPorRol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Reglas de perfil, cuentas de local, inicio de sesión y registro de empresa con la base H2 real. */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class IdentidadServiciosTests {
    private static final String CLAVE = "ClaveSegura1";
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2};
    private static final byte[] JPG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0, 1};

    @Autowired UsuarioRepository usuarios;
    @Autowired EstablecimientoRepository establecimientos;
    @Autowired CuentaEstablecimientoRepository cuentasRepo;
    @Autowired CodificadorContrasena codificador;
    @Autowired PerfilService perfiles;
    @Autowired CuentaLocalService cuentas;
    @Autowired SesionUsuarioService sesiones;
    @Autowired EmpresaService empresas;
    @Autowired RegistroUsuarioService registro;
    @Autowired UsuarioSesionActual usuarioActual;
    @Autowired CuentaLocalActual cuentaActual;
    @Autowired TransactionTemplate transacciones;

    // ---------------------------------------------------------------- Perfil

    @Test
    void perfil_actualizaDatosLimpiandoEspaciosYNormalizandoElCorreo() {
        Usuario usuario = usuario(true);
        String nuevo = "Nuevo." + UUID.randomUUID() + "@Example.TEST";

        var respuesta = perfiles.actualizar(usuario.getId(),
                new PerfilRequest("  Lucía ", " Pérez  ", "  " + nuevo + " ", " 999888777 "));

        assertEquals("Lucía", respuesta.nombres());
        assertEquals("Pérez", respuesta.apellidos());
        assertEquals(nuevo.toLowerCase(), respuesta.email());
        assertEquals("999888777", respuesta.telefono());
        assertFalse(respuesta.tieneFoto());
        assertEquals(nuevo.toLowerCase(), usuarios.findById(usuario.getId()).orElseThrow().getEmail());
    }

    @Test
    void perfil_telefonoVacioONuloSeGuardaComoNulo_yPuedeConservarSuMismoCorreo() {
        Usuario usuario = usuario(true);

        var conBlanco = perfiles.actualizar(usuario.getId(), new PerfilRequest("Ana", "Soto", usuario.getEmail(), "   "));
        var conNulo = perfiles.actualizar(usuario.getId(), new PerfilRequest("Ana", "Soto", usuario.getEmail(), null));

        assertNull(conBlanco.telefono());
        assertNull(conNulo.telefono());
        assertEquals(usuario.getEmail(), conNulo.email());
    }

    @Test
    void perfil_noPermiteUsarElCorreoDeOtraPersona() {
        Usuario primero = usuario(true);
        Usuario segundo = usuario(true);

        assertStatus(HttpStatus.CONFLICT, () -> perfiles.actualizar(segundo.getId(),
                new PerfilRequest("Ana", "Soto", primero.getEmail().toUpperCase(), null)));
    }

    @Test
    void perfil_usuarioInexistenteOInactivoRecibe401() {
        Usuario inactivo = usuario(false);
        PerfilRequest datos = new PerfilRequest("Ana", "Soto", UUID.randomUUID() + "@example.test", null);

        assertStatus(HttpStatus.UNAUTHORIZED, () -> perfiles.actualizar(-1L, datos));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> perfiles.actualizar(inactivo.getId(), datos));
    }

    @Test
    void perfil_cambiaLaContrasenaSoloSiLaActualCoincide() {
        Usuario usuario = usuario(true);

        assertStatus(HttpStatus.BAD_REQUEST, () -> perfiles.cambiarContrasena(usuario.getId(),
                new CambioContrasenaRequest("otra-clave", "ClaveNueva123")));
        perfiles.cambiarContrasena(usuario.getId(), new CambioContrasenaRequest(CLAVE, "ClaveNueva123"));

        String hash = usuarios.findById(usuario.getId()).orElseThrow().getPasswordHash();
        assertTrue(codificador.coincide("ClaveNueva123", hash));
        assertFalse(codificador.coincide(CLAVE, hash));
    }

    @Test
    void perfil_guardaFotoPngYJpgConSuTipo() {
        Usuario usuario = usuario(true);

        perfiles.guardarFoto(usuario.getId(), new MockMultipartFile("archivo", "f.png", "image/png", PNG));
        assertEquals(new PerfilService.Foto("image/png", PNG.clone()), perfiles.foto(usuario.getId()));
        assertTrue(sesiones.obtener(usuario.getId()).tieneFoto());

        perfiles.guardarFoto(usuario.getId(), new MockMultipartFile("archivo", "f.jpg", "image/jpeg", JPG));
        var foto = perfiles.foto(usuario.getId());
        assertEquals("image/jpeg", foto.tipo());
        assertArrayEquals(JPG, foto.contenido());
    }

    @Test
    void perfil_rechazaFotoAusenteVaciaOMayorADosMegas() {
        Long id = usuario(true).getId();

        assertStatus(HttpStatus.BAD_REQUEST, () -> perfiles.guardarFoto(id, null));
        assertStatus(HttpStatus.BAD_REQUEST, () -> perfiles.guardarFoto(id,
                new MockMultipartFile("archivo", "f.png", "image/png", new byte[0])));
        byte[] grande = new byte[2_000_001];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);
        assertStatus(HttpStatus.BAD_REQUEST, () -> perfiles.guardarFoto(id,
                new MockMultipartFile("archivo", "f.png", "image/png", grande)));
    }

    @Test
    void perfil_rechazaArchivosQueNoSonPngNiJpgAunqueSeParezcan() {
        Long id = usuario(true).getId();

        // Cada byte de la firma PNG alterado por separado deja de ser una imagen válida.
        for (int i = 0; i < 8; i++) {
            byte[] casiPng = PNG.clone();
            casiPng[i] = 0x00;
            assertStatus(HttpStatus.BAD_REQUEST, () -> perfiles.guardarFoto(id, archivo(casiPng)));
        }
        // Lo mismo con la firma JPG.
        for (int i = 0; i < 3; i++) {
            byte[] casiJpg = JPG.clone();
            casiJpg[i] = 0x00;
            assertStatus(HttpStatus.BAD_REQUEST, () -> perfiles.guardarFoto(id, archivo(casiJpg)));
        }
        // Firmas incompletas y texto plano.
        assertStatus(HttpStatus.BAD_REQUEST, () -> perfiles.guardarFoto(id,
                archivo(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a})));
        assertStatus(HttpStatus.BAD_REQUEST, () -> perfiles.guardarFoto(id, archivo(new byte[]{(byte) 0xff, (byte) 0xd8})));
        assertStatus(HttpStatus.BAD_REQUEST, () -> perfiles.guardarFoto(id, archivo("hola".getBytes())));
        assertStatus(HttpStatus.NOT_FOUND, () -> perfiles.foto(id));
    }

    @Test
    void perfil_siNoSePuedeLeerElArchivoResponde400() throws IOException {
        Long id = usuario(true).getId();
        MultipartFile roto = mock(MultipartFile.class);
        when(roto.isEmpty()).thenReturn(false);
        when(roto.getSize()).thenReturn(10L);
        when(roto.getBytes()).thenThrow(new IOException("disco"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> perfiles.guardarFoto(id, roto));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertInstanceOf(IOException.class, error.getCause());
    }

    @Test
    void perfil_fotoInexistenteResponde404() {
        Long id = usuario(true).getId();

        assertStatus(HttpStatus.NOT_FOUND, () -> perfiles.foto(id));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> perfiles.foto(-1L));
    }

    @Test
    void perfil_laFotoSeComparaPorContenidoYNoPorReferencia() {
        var a = new PerfilService.Foto("image/png", new byte[]{1, 2, 3});
        var b = new PerfilService.Foto("image/png", new byte[]{1, 2, 3});
        var otroTipo = new PerfilService.Foto("image/jpeg", new byte[]{1, 2, 3});
        var otroContenido = new PerfilService.Foto("image/png", new byte[]{9});

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, otroTipo);
        assertNotEquals(a, otroContenido);
        Object otroObjeto = "image/png";
        assertNotEquals(a, otroObjeto);
        assertEquals("Foto[tipo=image/png, contenido=3 bytes]", a.toString());
        assertEquals("Foto[tipo=null, contenido=0 bytes]", new PerfilService.Foto(null, null).toString());
    }

    // ---------------------------------------------------------------- Sesión de persona

    @Test
    void sesion_iniciaConCorreoSinImportarMayusculasNiEspacios() {
        Usuario usuario = usuario(true);

        var respuesta = sesiones.iniciar(new InicioSesionRequest("  " + usuario.getEmail().toUpperCase() + " ", CLAVE));

        assertEquals(usuario.getId(), respuesta.id());
        assertEquals("Prueba", respuesta.nombres());
        assertFalse(respuesta.tieneFoto());
    }

    @Test
    void sesion_rechazaCorreoInexistenteClaveIncorrectaYCuentaInactiva() {
        Usuario inactivo = usuario(false);
        Usuario activo = usuario(true);

        assertStatus(HttpStatus.UNAUTHORIZED, () -> sesiones.iniciar(
                new InicioSesionRequest(UUID.randomUUID() + "@example.test", CLAVE)));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> sesiones.iniciar(new InicioSesionRequest(activo.getEmail(), "mala")));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> sesiones.iniciar(new InicioSesionRequest(inactivo.getEmail(), CLAVE)));
    }

    @Test
    void sesion_bloqueaCon429TrasCincoFallos_inclusoConLaClaveCorrecta() {
        Usuario usuario = usuario(true);
        for (int i = 0; i < 5; i++)
            assertStatus(HttpStatus.UNAUTHORIZED, () -> sesiones.iniciar(new InicioSesionRequest(usuario.getEmail(), "mala")));

        assertStatus(HttpStatus.TOO_MANY_REQUESTS, () -> sesiones.iniciar(new InicioSesionRequest(usuario.getEmail(), CLAVE)));
    }

    @Test
    void sesion_obtenerRechazaUsuarioInexistenteOInactivo() {
        Usuario activo = usuario(true);
        Usuario inactivo = usuario(false);

        assertEquals(activo.getEmail(), sesiones.obtener(activo.getId()).email());
        assertStatus(HttpStatus.UNAUTHORIZED, () -> sesiones.obtener(-1L));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> sesiones.obtener(inactivo.getId()));
    }

    @Test
    void usuarioSesionActual_exigeUnIdNumericoDeUnaCuentaActiva() {
        Usuario activo = usuario(true);
        Usuario inactivo = usuario(false);

        assertStatus(HttpStatus.UNAUTHORIZED, () -> usuarioActual.id(new MockHttpServletRequest()));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> usuarioActual.id(peticion(SesionesPorRol.PERSONA, "texto")));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> usuarioActual.id(peticion(SesionesPorRol.PERSONA, inactivo.getId())));
        assertEquals(activo.getId(), usuarioActual.id(peticion(SesionesPorRol.PERSONA, activo.getId())));
    }

    // ---------------------------------------------------------------- Registro

    @Test
    void registro_normalizaElCorreoYNoPermiteDuplicados() {
        String correo = "Nueva." + UUID.randomUUID() + "@Example.TEST";

        var creado = registro.registrar(new RegistroUsuarioRequest(" Ana ", " Soto ", " " + correo + " ", CLAVE, " 987 "));
        assertEquals(correo.toLowerCase(), creado.email());
        assertEquals("Ana", creado.nombres());
        assertEquals("987", creado.telefono());
        var sinTelefono = registro.registrar(new RegistroUsuarioRequest("Ana", "Soto",
                UUID.randomUUID() + "@example.test", CLAVE, null));
        assertNull(sinTelefono.telefono());

        assertStatus(HttpStatus.CONFLICT, () -> registro.registrar(
                new RegistroUsuarioRequest("Otra", "Persona", correo.toUpperCase(), CLAVE, null)));
    }

    // ---------------------------------------------------------------- Cuenta del local

    @Test
    void cuentaLocal_seCreaSoloEnUnLocalAprobadoDelPropioDueno() {
        Long dueno = usuario(true).getId();
        Long otro = usuario(true).getId();
        Long pendiente = local(dueno, EstadoEstablecimiento.PENDIENTE_APROBACION);
        Long activo = local(dueno, EstadoEstablecimiento.ACTIVO);
        String nombre = "Caja-" + UUID.randomUUID();

        assertStatus(HttpStatus.NOT_FOUND, () -> cuentas.crear(dueno, -1L, new CuentaLocalRequest("x", CLAVE)));
        assertStatus(HttpStatus.NOT_FOUND, () -> cuentas.crear(otro, activo, new CuentaLocalRequest("x", CLAVE)));
        assertStatus(HttpStatus.CONFLICT, () -> cuentas.crear(dueno, pendiente, new CuentaLocalRequest("x", CLAVE)));

        CuentaEstablecimiento cuenta = cuentas.crear(dueno, activo, new CuentaLocalRequest("  " + nombre + " ", CLAVE));
        assertEquals(nombre.toLowerCase(), cuenta.getNombreUsuario());
        assertEquals(activo, cuenta.getEstablecimiento().getId());
        assertTrue(codificador.coincide(CLAVE, cuenta.getPasswordHash()));

        assertStatus(HttpStatus.CONFLICT, () -> cuentas.crear(dueno, activo,
                new CuentaLocalRequest("otra-" + UUID.randomUUID(), CLAVE)));
    }

    @Test
    void cuentaLocal_unLocalSuspendidoPuedeTenerCuenta_peroNoSeRepiteElNombreDeUsuario() {
        Long dueno = usuario(true).getId();
        Long suspendido = local(dueno, EstadoEstablecimiento.SUSPENDIDO);
        Long activo = local(dueno, EstadoEstablecimiento.ACTIVO);
        String nombre = "caja-" + UUID.randomUUID();

        assertNotNull(cuentas.crear(dueno, suspendido, new CuentaLocalRequest(nombre, CLAVE)).getId());
        assertStatus(HttpStatus.CONFLICT, () -> cuentas.crear(dueno, activo, new CuentaLocalRequest(nombre.toUpperCase(), CLAVE)));
    }

    @Test
    void cuentaLocal_iniciaSesionYRegistraElUltimoAcceso() {
        Long dueno = usuario(true).getId();
        Long local = local(dueno, EstadoEstablecimiento.ACTIVO);
        String nombre = "caja-" + UUID.randomUUID();
        cuentas.crear(dueno, local, new CuentaLocalRequest(nombre, CLAVE));

        CuentaEstablecimiento cuenta = cuentas.iniciar(new CuentaLocalRequest(" " + nombre.toUpperCase() + " ", CLAVE));

        assertEquals(nombre, cuenta.getNombreUsuario());
        assertNotNull(cuentasRepo.findById(cuenta.getId()).orElseThrow().getUltimoAcceso());
        assertEquals(cuenta.getId(), cuentas.obtener(cuenta.getId()).getId());
    }

    @Test
    void cuentaLocal_rechazaUsuarioInexistenteClaveIncorrectaYCuentaDesactivada() {
        Long dueno = usuario(true).getId();
        Long local = local(dueno, EstadoEstablecimiento.ACTIVO);
        String nombre = "caja-" + UUID.randomUUID();
        CuentaEstablecimiento cuenta = cuentas.crear(dueno, local, new CuentaLocalRequest(nombre, CLAVE));

        assertStatus(HttpStatus.UNAUTHORIZED, () -> cuentas.iniciar(new CuentaLocalRequest("nadie-" + UUID.randomUUID(), CLAVE)));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> cuentas.iniciar(new CuentaLocalRequest(nombre, "clave-mala")));

        CuentaEstablecimiento guardada = cuentasRepo.findById(cuenta.getId()).orElseThrow();
        guardada.setActiva(false);
        cuentasRepo.save(guardada);
        assertStatus(HttpStatus.UNAUTHORIZED, () -> cuentas.iniciar(new CuentaLocalRequest(nombre, CLAVE)));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> cuentas.obtener(cuenta.getId()));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> cuentas.obtener(-1L));
    }

    @Test
    void cuentaLocal_unLocalInactivoYaNoPuedeOperar() {
        Long dueno = usuario(true).getId();
        Long local = local(dueno, EstadoEstablecimiento.ACTIVO);
        String nombre = "caja-" + UUID.randomUUID();
        CuentaEstablecimiento cuenta = cuentas.crear(dueno, local, new CuentaLocalRequest(nombre, CLAVE));

        cambiarEstado(local, EstadoEstablecimiento.SUSPENDIDO);
        assertEquals(cuenta.getId(), cuentas.iniciar(new CuentaLocalRequest(nombre, CLAVE)).getId());

        cambiarEstado(local, EstadoEstablecimiento.INACTIVO);
        assertStatus(HttpStatus.UNAUTHORIZED, () -> cuentas.iniciar(new CuentaLocalRequest(nombre, CLAVE)));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> cuentas.obtener(cuenta.getId()));
    }

    @Test
    void cuentaLocalActual_exigeUnaSesionDeLocalValida() {
        Long dueno = usuario(true).getId();
        Long local = local(dueno, EstadoEstablecimiento.ACTIVO);
        CuentaEstablecimiento cuenta = cuentas.crear(dueno, local, new CuentaLocalRequest("caja-" + UUID.randomUUID(), CLAVE));

        assertStatus(HttpStatus.UNAUTHORIZED, () -> cuentaActual.obtener(new MockHttpServletRequest()));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> cuentaActual.obtener(peticion(SesionesPorRol.LOCAL, "1")));
        assertEquals(cuenta.getId(), cuentaActual.obtener(peticion(SesionesPorRol.LOCAL, cuenta.getId())).getId());
    }

    // ---------------------------------------------------------------- Empresa y locales

    @Test
    void empresa_registraConRucLimpioYUnaSolaPorCuenta() {
        Long dueno = usuario(true).getId();
        String ruc = ruc();

        assertTrue(empresas.obtenerEmpresa(dueno).isEmpty());
        var empresa = empresas.registrarEmpresa(dueno, new EmpresaRequest(" Razón SAC ", " Comercio ", " " + ruc + " "));
        assertEquals(ruc, empresa.ruc());
        assertEquals("Razón SAC", empresa.razonSocial());
        assertEquals("Comercio", empresa.nombreComercial());
        assertEquals(empresa, empresas.obtenerEmpresa(dueno).orElseThrow());

        assertStatus(HttpStatus.CONFLICT, () -> empresas.registrarEmpresa(dueno, new EmpresaRequest("Otra", "Otra", ruc())));
    }

    @Test
    void empresa_rechazaRucDuplicadoYUsuarioInexistenteOInactivo() {
        Long dueno = usuario(true).getId();
        String ruc = ruc();
        empresas.registrarEmpresa(dueno, new EmpresaRequest("Razón", "Comercio", ruc));
        Long otro = usuario(true).getId();
        Long inactivo = usuario(false).getId();

        assertStatus(HttpStatus.CONFLICT, () -> empresas.registrarEmpresa(otro, new EmpresaRequest("Razón", "Comercio", ruc)));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> empresas.registrarEmpresa(-1L, new EmpresaRequest("R", "C", ruc())));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> empresas.registrarEmpresa(inactivo, new EmpresaRequest("R", "C", ruc())));
    }

    @Test
    void empresa_sinEmpresaNoSePuedenRegistrarNiListarLocales() {
        Long sinEmpresa = usuario(true).getId();

        assertStatus(HttpStatus.FORBIDDEN, () -> empresas.registrarEstablecimiento(sinEmpresa, establecimiento("Local")));
        assertStatus(HttpStatus.FORBIDDEN, () -> empresas.listarEstablecimientos(sinEmpresa));
        assertStatus(HttpStatus.FORBIDDEN, () -> empresas.actualizarUbicacion(sinEmpresa, 1L,
                new UbicacionRequest(BigDecimal.ONE, BigDecimal.ONE)));
    }

    @Test
    void empresa_registraLocalesConCamposOpcionalesLimpiosYLosListaEnOrden() {
        Long dueno = usuario(true).getId();
        empresas.registrarEmpresa(dueno, new EmpresaRequest("Razón", "Comercio", ruc()));

        var primero = empresas.registrarEstablecimiento(dueno, new EstablecimientoRequest(" Panadería ", " Pan del día ",
                " 012345 ", new DireccionRequest(" Lima ", " Lima ", " Miraflores ", " Av. Larco ", " 123 ", " Frente al parque ",
                " 15074 ", new BigDecimal("-12.1200000"), new BigDecimal("-77.0300000"))));
        var segundo = empresas.registrarEstablecimiento(dueno, establecimiento("Cafetería"));

        assertEquals("Panadería", primero.nombre());
        assertEquals("Miraflores", primero.distrito());
        assertEquals("Av. Larco", primero.direccion());
        assertEquals(EstadoEstablecimiento.PENDIENTE_APROBACION, primero.estado());
        assertNull(primero.cuentaTrabajador());
        assertEquals(0, new BigDecimal("-12.12").compareTo(primero.latitud()));
        transacciones.executeWithoutResult(estado -> {
            Establecimiento guardado = establecimientos.findById(primero.id()).orElseThrow();
            assertEquals("Pan del día", guardado.getDescripcion());
            assertEquals("012345", guardado.getTelefono());
            assertEquals("123", guardado.getDireccion().getNumero());
            assertEquals("Frente al parque", guardado.getDireccion().getReferencia());
            assertEquals("15074", guardado.getDireccion().getCodigoPostal());
            Establecimiento sinOpcionales = establecimientos.findById(segundo.id()).orElseThrow();
            assertNull(sinOpcionales.getDescripcion());
            assertNull(sinOpcionales.getTelefono());
            assertNull(sinOpcionales.getDireccion().getNumero());
            assertNull(sinOpcionales.getDireccion().getReferencia());
        });
        assertNull(segundo.latitud());

        var lista = empresas.listarEstablecimientos(dueno);
        assertEquals(2, lista.size());
        assertEquals(primero.id(), lista.get(0).id());
        assertEquals(segundo.id(), lista.get(1).id());
    }

    @Test
    void empresa_elListadoMuestraLaCuentaDelTrabajador() {
        Long dueno = usuario(true).getId();
        Long local = local(dueno, EstadoEstablecimiento.ACTIVO);
        String nombre = "caja-" + UUID.randomUUID();
        cuentas.crear(dueno, local, new CuentaLocalRequest(nombre, CLAVE));

        assertEquals(nombre, empresas.listarEstablecimientos(dueno).getFirst().cuentaTrabajador());
    }

    @Test
    void empresa_soloElDuenoActualizaLaUbicacionDeSuLocal() {
        Long dueno = usuario(true).getId();
        Long local = local(dueno, EstadoEstablecimiento.PENDIENTE_APROBACION);
        Long otro = usuario(true).getId();
        local(otro, EstadoEstablecimiento.PENDIENTE_APROBACION);
        UbicacionRequest ubicacion = new UbicacionRequest(new BigDecimal("-12.05"), new BigDecimal("-77.04"));

        var actualizado = empresas.actualizarUbicacion(dueno, local, ubicacion);
        assertEquals(0, new BigDecimal("-12.05").compareTo(actualizado.latitud()));
        assertEquals(0, new BigDecimal("-77.04").compareTo(actualizado.longitud()));
        assertEquals(0, new BigDecimal("-12.05").compareTo(transacciones.execute(estado ->
                establecimientos.findById(local).orElseThrow().getDireccion().getLatitud())));

        assertStatus(HttpStatus.NOT_FOUND, () -> empresas.actualizarUbicacion(otro, local, ubicacion));
        assertStatus(HttpStatus.NOT_FOUND, () -> empresas.actualizarUbicacion(dueno, -1L, ubicacion));
    }

    // ---------------------------------------------------------------- Ayudas

    private Usuario usuario(boolean activo) {
        return usuarios.save(Usuario.builder().nombres("Prueba").apellidos("Identidad")
                .email(UUID.randomUUID() + "@example.test").passwordHash(codificador.codificar(CLAVE))
                .activo(activo).build());
    }

    /** Crea (si hace falta) la empresa del dueño y un local en el estado pedido. */
    private Long local(Long dueno, EstadoEstablecimiento estado) {
        if (empresas.obtenerEmpresa(dueno).isEmpty())
            empresas.registrarEmpresa(dueno, new EmpresaRequest("Empresa", "Comercio", ruc()));
        Long id = empresas.registrarEstablecimiento(dueno, establecimiento("Local " + estado)).id();
        cambiarEstado(id, estado);
        return id;
    }

    private void cambiarEstado(Long localId, EstadoEstablecimiento estado) {
        Establecimiento local = establecimientos.findById(localId).orElseThrow();
        local.setEstado(estado);
        establecimientos.save(local);
    }

    private static EstablecimientoRequest establecimiento(String nombre) {
        return new EstablecimientoRequest(nombre, "  ", null,
                new DireccionRequest("Lima", "Lima", "Surquillo", "Calle Cinco", " ", null, null));
    }

    private static String ruc() {
        return Long.toString(ThreadLocalRandom.current().nextLong(10_000_000_000L, 100_000_000_000L));
    }

    private static MockMultipartFile archivo(byte[] bytes) {
        return new MockMultipartFile("archivo", "foto", "application/octet-stream", bytes);
    }

    private static MockHttpServletRequest peticion(String atributo, Object valor) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(atributo, valor);
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setSession(sesion);
        return peticion;
    }

    private static void assertStatus(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado, error.getStatusCode());
    }
}
