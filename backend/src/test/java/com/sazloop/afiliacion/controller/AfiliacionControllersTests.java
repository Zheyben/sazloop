package com.sazloop.afiliacion.controller;

import com.sazloop.afiliacion.ApoyoAfiliacion;
import com.sazloop.afiliacion.domain.AdministradorPlataforma;
import com.sazloop.afiliacion.domain.DocumentoEstablecimiento.TipoDocumento;
import com.sazloop.afiliacion.dto.SolicitudResponse;
import com.sazloop.shared.SesionesPorRol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Rutas HTTP de afiliación: sesión obligatoria, códigos de respuesta y tipo de archivo descargado. */
@SpringBootTest
@TestPropertySource(properties = "sazloop.documentos.directorio=target/test-documentos")
class AfiliacionControllersTests extends ApoyoAfiliacion {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 7, 7};
    private static final byte[] JPG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0, 9};

    @Autowired WebApplicationContext contexto;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void lasRutasDelAdministradorExigenSesion() throws Exception {
        mvc.perform(get("/api/administradores/solicitudes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/administradores/establecimientos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/administradores/solicitudes/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/administradores/documentos/1/archivo")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/administradores/documentos/1/revision").contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultado\":\"APROBADO\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/administradores/solicitudes/1/decision").contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APROBADA\",\"motivo\":\"Conforme\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/administradores/solicitudes/1/resolucion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentos\":[{\"id\":1,\"resultado\":\"APROBADO\"}]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elAdministradorRevisaYDecideDesdeLaApi() throws Exception {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local API revisión");
        adjuntar(propietario, local, TipoDocumento.CERTIFICADO_SANITARIO, "CS-API", hoy().plusYears(1));
        SolicitudResponse solicitud = solicitudes.adjuntar(propietario, local, TipoDocumento.OTRO, null, null, null,
                new MockMultipartFile("archivo", "foto.png", "image/png", PNG));
        solicitudes.enviar(propietario, local);
        Long certificado = solicitud.documentos().get(0).id();
        Long foto = solicitud.documentos().get(1).id();
        MockHttpSession sesion = sesionAdministrador(admin());

        mvc.perform(get("/api/administradores/solicitudes").session(sesion))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(solicitud.id().intValue())));
        mvc.perform(get("/api/administradores/establecimientos").session(sesion))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(local.intValue())));
        mvc.perform(get("/api/administradores/solicitudes/{id}", solicitud.id()).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.establecimiento").value("Local API revisión"))
                .andExpect(jsonPath("$.documentos.length()").value(2));
        mvc.perform(get("/api/administradores/solicitudes/{id}", -1).session(sesion))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/administradores/documentos/{id}/archivo", certificado).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documento.pdf\""))
                .andExpect(content().bytes(PDF));
        mvc.perform(get("/api/administradores/documentos/{id}/archivo", foto).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));

        // Validación del cuerpo: falta el resultado.
        mvc.perform(post("/api/administradores/documentos/{id}/revision", certificado).session(sesion)
                .contentType(MediaType.APPLICATION_JSON).content("{\"observaciones\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/administradores/documentos/{id}/revision", certificado).session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultado\":\"APROBADO\",\"observaciones\":\"Legible\",\"numero\":\"CS-API-2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos[0].resultado").value("APROBADO"))
                .andExpect(jsonPath("$.documentos[0].numero").value("CS-API-2"));
        mvc.perform(post("/api/administradores/documentos/{id}/revision", foto).session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultado\":\"RECHAZADO\",\"observaciones\":\"No corresponde\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/administradores/solicitudes/{id}/decision", solicitud.id()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"RECHAZADA\",\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/administradores/solicitudes/{id}/decision", solicitud.id()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APROBADA\",\"motivo\":\"Conforme\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/administradores/solicitudes/{id}/decision", solicitud.id()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"RECHAZADA\",\"motivo\":\"Adjunte la licencia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoDecision").value("Adjunte la licencia"));
    }

    @Test
    void elAdministradorResuelveEnUnPasoYDescargaJpg() throws Exception {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local API resolución");
        SolicitudResponse solicitud = solicitudes.adjuntar(propietario, local, TipoDocumento.OTRO, null, null, null,
                new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", JPG));
        solicitudes.enviar(propietario, local);
        Long documento = solicitud.documentos().getFirst().id();
        MockHttpSession sesion = sesionAdministrador(admin());

        mvc.perform(get("/api/administradores/documentos/{id}/archivo", documento).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documento.jpg\""));
        mvc.perform(post("/api/administradores/solicitudes/{id}/resolucion", solicitud.id()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON).content("{\"documentos\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/administradores/solicitudes/{id}/resolucion", solicitud.id()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentos\":[{\"id\":" + documento + ",\"resultado\":\"APROBADO\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.estadoLocal").value("ACTIVO"));
    }

    @Test
    void unAdministradorDadoDeBajaPierdeElAcceso() throws Exception {
        AdministradorPlataforma admin = admin();
        admin.setActivo(false);
        administradores.save(admin);
        mvc.perform(get("/api/administradores/solicitudes").session(sesionAdministrador(admin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sesionDelAdministradorSeConsultaYSeCierra() throws Exception {
        AdministradorPlataforma admin = admin();
        mvc.perform(get("/api/administradores/sesion")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/administradores/sesion")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/administradores/sesion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"no-es-correo\",\"contrasena\":\"\"}")).andExpect(status().isBadRequest());

        MockHttpSession sesion = sesionAdministrador(admin);
        mvc.perform(get("/api/administradores/sesion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId().intValue()))
                .andExpect(jsonPath("$.email").value(admin.getEmailInstitucional()))
                .andExpect(jsonPath("$.nombre").value("Revisor de prueba"));
        mvc.perform(delete("/api/administradores/sesion").session(sesion)).andExpect(status().isNoContent());
        mvc.perform(get("/api/administradores/sesion").session(sesion)).andExpect(status().isUnauthorized());
    }

    @Test
    void lasRutasDelPropietarioExigenSesion() throws Exception {
        mvc.perform(get("/api/establecimientos/1/solicitud")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/establecimientos/1/documentacion")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/establecimientos/1/solicitud/enviar")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/establecimientos/1/documentos/1/archivo")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/establecimientos/1/documentos").file(new MockMultipartFile("archivo",
                "a.pdf", "application/pdf", PDF)).param("tipo", "OTRO")).andExpect(status().isUnauthorized());
    }

    @Test
    void elPropietarioAdjuntaEnviaYDescargaDesdeLaApi() throws Exception {
        Long propietario = usuario();
        Long local = localPendiente(propietario, "Local API propietario");
        MockHttpSession sesion = sesionPropietario(propietario);

        mvc.perform(get("/api/establecimientos/{id}/solicitud", local).session(sesion))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/establecimientos/{id}/documentacion", local).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_APROBACION"))
                .andExpect(jsonPath("$.solicitud").value(org.hamcrest.Matchers.nullValue()));

        mvc.perform(multipart("/api/establecimientos/{id}/documentos", local).session(sesion)
                        .file(new MockMultipartFile("archivo", "licencia.pdf", "application/pdf", PDF))
                        .param("tipo", "LICENCIA_FUNCIONAMIENTO").param("numero", "LF-API")
                        .param("emision", hoy().minusYears(1).toString())
                        .param("vencimiento", hoy().plusYears(1).toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.documentos[0].numero").value("LF-API"));
        mvc.perform(multipart("/api/establecimientos/{id}/documentos", local).session(sesion)
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", PNG)).param("tipo", "OTRO"))
                .andExpect(status().isCreated());
        String json = mvc.perform(multipart("/api/establecimientos/{id}/documentos", local).session(sesion)
                        .file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", JPG)).param("tipo", "OTRO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentos.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        // Un archivo que no es PDF, PNG ni JPG se rechaza.
        mvc.perform(multipart("/api/establecimientos/{id}/documentos", local).session(sesion)
                        .file(new MockMultipartFile("archivo", "virus.exe", "application/octet-stream",
                                "MZ ejecutable".getBytes())).param("tipo", "OTRO"))
                .andExpect(status().isBadRequest());

        Long[] ids = idsDocumentos(json);
        mvc.perform(get("/api/establecimientos/{l}/documentos/{d}/archivo", local, ids[0]).session(sesion))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(PDF));
        mvc.perform(get("/api/establecimientos/{l}/documentos/{d}/archivo", local, ids[1]).session(sesion))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));
        mvc.perform(get("/api/establecimientos/{l}/documentos/{d}/archivo", local, ids[2]).session(sesion))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documento.jpg\""));

        mvc.perform(post("/api/establecimientos/{id}/solicitud/enviar", local).session(sesion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_REVISION"));
        mvc.perform(get("/api/establecimientos/{id}/solicitud", local).session(sesion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_REVISION"));

        // Otra persona no ve ni descarga nada del local.
        MockHttpSession ajena = sesionPropietario(usuario());
        mvc.perform(get("/api/establecimientos/{id}/solicitud", local).session(ajena))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/establecimientos/{l}/documentos/{d}/archivo", local, ids[0]).session(ajena))
                .andExpect(status().isNotFound());
    }

    private static Long[] idsDocumentos(String json) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{\"id\":(\\d+),\"tipo\"").matcher(json);
        java.util.List<Long> ids = new java.util.ArrayList<>();
        while (m.find()) ids.add(Long.valueOf(m.group(1)));
        return ids.toArray(Long[]::new);
    }

    private static MockHttpSession sesionAdministrador(AdministradorPlataforma admin) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(SesionesPorRol.ADMINISTRADOR, admin.getId());
        return sesion;
    }

    private static MockHttpSession sesionPropietario(Long usuarioId) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(SesionesPorRol.PERSONA, usuarioId);
        return sesion;
    }
}
