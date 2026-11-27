package com.sazloop.identidad;

import com.sazloop.afiliacion.domain.AdministradorPlataforma;
import com.sazloop.afiliacion.repository.AdministradorPlataformaRepository;
import com.sazloop.identidad.domain.Establecimiento;
import com.sazloop.identidad.domain.Usuario;
import com.sazloop.identidad.repository.EstablecimientoRepository;
import com.sazloop.identidad.repository.UsuarioRepository;
import com.sazloop.identidad.service.CodificadorContrasena;
import com.sazloop.shared.SesionesPorRol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas HTTP (MockMvc) de los controladores de identidad y de la sesión del administrador. */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class IdentidadControladoresTests {
    private static final String CLAVE = "ClaveSegura1";
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 7, 7};

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarios;
    @Autowired EstablecimientoRepository establecimientos;
    @Autowired AdministradorPlataformaRepository administradores;
    @Autowired CodificadorContrasena codificador;
    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    // ---------------------------------------------------------------- Registro y sesión de persona

    @Test
    void registro_creaLaCuentaYRechazaDuplicadosODatosInvalidos() throws Exception {
        String correo = UUID.randomUUID() + "@example.test";

        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(registroJson(correo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(correo))
                .andExpect(jsonPath("$.tieneFoto").value(false))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(registroJson(correo)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(registroJson("no-es-correo")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sesion_iniciaConsultaYCierra() throws Exception {
        Usuario usuario = usuario();
        MockHttpSession sesion = new MockHttpSession();

        mvc.perform(post("/api/sesion").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content(credencialesJson(usuario.getEmail(), CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getId()));
        assertEquals(usuario.getId(), sesion.getAttribute(SesionesPorRol.PERSONA));

        mvc.perform(get("/api/sesion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(usuario.getEmail()));
        mvc.perform(delete("/api/sesion").session(sesion)).andExpect(status().isNoContent());
        assertTrue(sesion.isInvalid());
    }

    @Test
    void sesion_sinIniciarOConDatoAjenoResponde401() throws Exception {
        MockHttpSession conTexto = new MockHttpSession();
        conTexto.setAttribute(SesionesPorRol.PERSONA, "1");

        mvc.perform(get("/api/sesion")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sesion").session(conTexto)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/sesion")).andExpect(status().isNoContent());
    }

    @Test
    void sesion_claveIncorrectaDa401YTrasCincoFallosDa429() throws Exception {
        Usuario usuario = usuario();
        for (int i = 0; i < 5; i++)
            mvc.perform(post("/api/sesion").contentType(MediaType.APPLICATION_JSON)
                    .content(credencialesJson(usuario.getEmail(), "mala"))).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/sesion").contentType(MediaType.APPLICATION_JSON)
                .content(credencialesJson(usuario.getEmail(), CLAVE))).andExpect(status().isTooManyRequests());
    }

    // ---------------------------------------------------------------- Perfil

    @Test
    void perfil_actualizaDatosYContrasena() throws Exception {
        Usuario usuario = usuario();
        MockHttpSession sesion = sesionPersona(usuario);
        String nuevo = UUID.randomUUID() + "@example.test";

        mvc.perform(put("/api/perfil").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Rosa\",\"apellidos\":\"Ríos\",\"email\":\"" + nuevo + "\",\"telefono\":\"900\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Rosa"))
                .andExpect(jsonPath("$.email").value(nuevo));
        mvc.perform(put("/api/perfil/contrasena").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"" + CLAVE + "\",\"nueva\":\"OtraClave99\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/perfil/contrasena").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"" + CLAVE + "\",\"nueva\":\"OtraClave99\"}"))
                .andExpect(status().isBadRequest());
        // La nueva contraseña debe tener al menos 8 caracteres.
        mvc.perform(put("/api/perfil/contrasena").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"OtraClave99\",\"nueva\":\"corta\"}"))
                .andExpect(status().isBadRequest());

        assertTrue(codificador.coincide("OtraClave99", usuarios.findById(usuario.getId()).orElseThrow().getPasswordHash()));
    }

    @Test
    void perfil_subeYDescargaLaFotoSinGuardarlaEnCache() throws Exception {
        MockHttpSession sesion = sesionPersona(usuario());

        mvc.perform(get("/api/perfil/foto").session(sesion)).andExpect(status().isNotFound());
        mvc.perform(multipart("/api/perfil/foto").file(new MockMultipartFile("archivo", "f.txt", "text/plain", "hola".getBytes()))
                .session(sesion)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/perfil/foto").file(new MockMultipartFile("archivo", "f.png", "image/png", PNG))
                .session(sesion)).andExpect(status().isNoContent());

        byte[] descargada = mvc.perform(get("/api/perfil/foto").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(PNG, descargada);
        mvc.perform(get("/api/sesion").session(sesion)).andExpect(jsonPath("$.tieneFoto").value(true));
    }

    @Test
    void perfil_sinSesionResponde401() throws Exception {
        mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"A\",\"apellidos\":\"B\",\"email\":\"a@example.test\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/perfil/foto")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- Empresa

    @Test
    void empresa_registraConsultaYGestionaLocales() throws Exception {
        MockHttpSession sesion = sesionPersona(usuario());
        String ruc = ruc();

        mvc.perform(get("/api/empresas/actual").session(sesion)).andExpect(status().isNoContent());
        mvc.perform(post("/api/empresas").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content(empresaJson("123")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/empresas").session(sesion).contentType(MediaType.APPLICATION_JSON).content(empresaJson(ruc)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruc").value(ruc));
        mvc.perform(post("/api/empresas").session(sesion).contentType(MediaType.APPLICATION_JSON).content(empresaJson(ruc())))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/empresas/actual").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("Razón SAC"));

        String cuerpo = mvc.perform(post("/api/empresas/actual/establecimientos").session(sesion)
                        .contentType(MediaType.APPLICATION_JSON).content(localJson("Panadería")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_APROBACION"))
                .andReturn().getResponse().getContentAsString();
        Long local = idDe(cuerpo);

        mvc.perform(put("/api/empresas/actual/establecimientos/" + local + "/ubicacion").session(sesion)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-12.1,\"longitud\":-77.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitud").value(-12.1));
        mvc.perform(put("/api/empresas/actual/establecimientos/" + local + "/ubicacion").session(sesion)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-120,\"longitud\":-77.0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/empresas/actual/establecimientos").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Panadería"));
        mvc.perform(get("/api/empresas/actual/indicadores").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.establecimientos", hasSize(1)))
                .andExpect(jsonPath("$.total.establecimiento").value("Total empresa"))
                .andExpect(jsonPath("$.total.ventas").value(0));
    }

    @Test
    void empresa_sinSesionOSinEmpresaNoPermiteGestionarLocales() throws Exception {
        mvc.perform(get("/api/empresas/actual")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/empresas/actual/establecimientos").session(sesionPersona(usuario()))
                        .contentType(MediaType.APPLICATION_JSON).content(localJson("Sin empresa")))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- Cuenta del local

    @Test
    void cuentaLocal_elDuenoLaCreaYElTrabajadorEntraConsultaYSale() throws Exception {
        MockHttpSession dueno = sesionPersona(usuario());
        mvc.perform(post("/api/empresas").session(dueno).contentType(MediaType.APPLICATION_JSON).content(empresaJson(ruc())))
                .andExpect(status().isCreated());
        Long local = idDe(mvc.perform(post("/api/empresas/actual/establecimientos").session(dueno)
                .contentType(MediaType.APPLICATION_JSON).content(localJson("Bodega")))
                .andReturn().getResponse().getContentAsString());
        String nombre = "caja-" + UUID.randomUUID();

        mvc.perform(post("/api/establecimientos/" + local + "/cuenta").session(dueno)
                        .contentType(MediaType.APPLICATION_JSON).content(cuentaJson(nombre, CLAVE)))
                .andExpect(status().isConflict());
        Establecimiento entidad = establecimientos.findById(local).orElseThrow();
        entidad.setEstado(Establecimiento.EstadoEstablecimiento.ACTIVO);
        establecimientos.save(entidad);
        mvc.perform(post("/api/establecimientos/" + local + "/cuenta").session(dueno)
                        .contentType(MediaType.APPLICATION_JSON).content(cuentaJson(nombre, CLAVE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreUsuario").value(nombre))
                .andExpect(jsonPath("$.establecimientoId").value(local));

        MockHttpSession trabajador = new MockHttpSession();
        mvc.perform(post("/api/local/sesion").session(trabajador).contentType(MediaType.APPLICATION_JSON)
                        .content(cuentaJson(nombre, "clave-incorrecta")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/local/sesion").session(trabajador).contentType(MediaType.APPLICATION_JSON)
                        .content(cuentaJson(nombre, CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value(nombre));
        mvc.perform(get("/api/local/sesion").session(trabajador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.establecimientoId").value(local));
        mvc.perform(delete("/api/local/sesion").session(trabajador)).andExpect(status().isNoContent());
        assertTrue(trabajador.isInvalid());
    }

    @Test
    void cuentaLocal_sinSesionResponde401() throws Exception {
        mvc.perform(get("/api/local/sesion")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/local/sesion")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/establecimientos/1/cuenta").contentType(MediaType.APPLICATION_JSON)
                .content(cuentaJson("caja", CLAVE))).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- Sesión del administrador

    @Test
    void administrador_entraConsultaYSale() throws Exception {
        String correo = UUID.randomUUID() + "@example.test";
        AdministradorPlataforma admin = administradores.save(AdministradorPlataforma.builder().nombre("Revisora")
                .emailInstitucional(correo).passwordHash(codificador.codificar(CLAVE)).build());
        MockHttpSession sesion = new MockHttpSession();

        mvc.perform(post("/api/administradores/sesion").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content(credencialesJson(correo, "mala")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/administradores/sesion").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content(credencialesJson(correo.toUpperCase(), CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()))
                .andExpect(jsonPath("$.nombre").value("Revisora"))
                .andExpect(jsonPath("$.email").value(correo));
        mvc.perform(get("/api/administradores/sesion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(correo));
        mvc.perform(delete("/api/administradores/sesion").session(sesion)).andExpect(status().isNoContent());
        assertTrue(sesion.isInvalid());
    }

    @Test
    void administrador_sinSesionODatosInvalidosEsRechazado() throws Exception {
        mvc.perform(get("/api/administradores/sesion")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/administradores/sesion")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/administradores/sesion").contentType(MediaType.APPLICATION_JSON)
                .content(credencialesJson("no-es-correo", CLAVE))).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- Ayudas

    private Usuario usuario() {
        return usuarios.save(Usuario.builder().nombres("Prueba").apellidos("Http")
                .email(UUID.randomUUID() + "@example.test").passwordHash(codificador.codificar(CLAVE)).build());
    }

    private MockHttpSession sesionPersona(Usuario usuario) throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/api/sesion").session(sesion).contentType(MediaType.APPLICATION_JSON)
                .content(credencialesJson(usuario.getEmail(), CLAVE))).andExpect(status().isOk());
        return sesion;
    }

    private static Long idDe(String json) {
        var m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
        assertTrue(m.find(), json);
        return Long.valueOf(m.group(1));
    }

    private static String registroJson(String correo) {
        return "{\"nombres\":\"Ana\",\"apellidos\":\"Soto\",\"email\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}";
    }

    private static String credencialesJson(String correo, String clave) {
        return "{\"email\":\"" + correo + "\",\"contrasena\":\"" + clave + "\"}";
    }

    private static String cuentaJson(String usuario, String clave) {
        return "{\"nombreUsuario\":\"" + usuario + "\",\"contrasena\":\"" + clave + "\"}";
    }

    private static String empresaJson(String ruc) {
        return "{\"razonSocial\":\"Razón SAC\",\"nombreComercial\":\"Comercio\",\"ruc\":\"" + ruc + "\"}";
    }

    private static String localJson(String nombre) {
        return "{\"nombre\":\"" + nombre + "\",\"direccion\":{\"departamento\":\"Lima\",\"provincia\":\"Lima\","
                + "\"distrito\":\"Surquillo\",\"direccionLinea\":\"Calle Cinco\"}}";
    }

    private static String ruc() {
        return Long.toString(ThreadLocalRandom.current().nextLong(10_000_000_000L, 100_000_000_000L));
    }
}
