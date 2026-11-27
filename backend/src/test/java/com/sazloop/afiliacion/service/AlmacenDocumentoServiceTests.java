package com.sazloop.afiliacion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Guardado privado de documentos: tipos permitidos por contenido, tamaño y lectura segura. */
class AlmacenDocumentoServiceTests {
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '4'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
    private static final byte[] JPG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0};

    @TempDir Path carpeta;
    private AlmacenDocumentoService almacen;

    @BeforeEach
    void preparar() {
        almacen = new AlmacenDocumentoService(carpeta.resolve("documentos").toString());
    }

    @Test
    void guardaPdfPngYJpgSegunSuContenidoYLosLee() {
        String pdf = almacen.guardar(archivo(PDF));
        String png = almacen.guardar(archivo(PNG));
        String jpg = almacen.guardar(archivo(JPG));

        assertTrue(pdf.matches("[a-f0-9-]{36}\\.pdf"));
        assertTrue(png.endsWith(".png"));
        assertTrue(jpg.endsWith(".jpg"));
        assertArrayEquals(PDF, almacen.leer(pdf));
        assertArrayEquals(PNG, almacen.leer(png));
        assertArrayEquals(JPG, almacen.leer(jpg));
    }

    @Test
    void rechazaArchivoAusenteVacioOMayorA10Mb() throws IOException {
        assertMensaje(HttpStatus.BAD_REQUEST, "Selecciona un documento de hasta 10 MB", null);
        assertMensaje(HttpStatus.BAD_REQUEST, "Selecciona un documento de hasta 10 MB", archivo(new byte[0]));
        MultipartFile enorme = mock(MultipartFile.class);
        when(enorme.isEmpty()).thenReturn(false);
        when(enorme.getSize()).thenReturn(10L * 1024 * 1024 + 1);
        assertMensaje(HttpStatus.BAD_REQUEST, "Selecciona un documento de hasta 10 MB", enorme);
        assertFalse(Files.exists(carpeta.resolve("documentos")));
    }

    @Test
    void rechazaContenidosQueNoSonPdfPngNiJpg() {
        byte[][] invalidos = {
                {'%', 'P'},                                   // demasiado corto
                {'%', 'X', 'D', 'F'}, {'%', 'P', 'X', 'F'}, {'%', 'P', 'D', 'X'},
                {'x', 'P', 'D', 'F'},
                {(byte) 0x89, 'P', 'N', 'G'},                 // cabecera PNG incompleta
                {(byte) 0x89, 'X', 'N', 'G', 0, 0, 0, 0}, {(byte) 0x89, 'P', 'X', 'G', 0, 0, 0, 0},
                {(byte) 0x89, 'P', 'N', 'X', 0, 0, 0, 0},
                {(byte) 0xff, (byte) 0xd8},                   // cabecera JPG incompleta
                {(byte) 0xff, 0x00, (byte) 0xff}, {(byte) 0xff, (byte) 0xd8, 0x00},
                "texto plano".getBytes()
        };
        for (byte[] contenido : invalidos) {
            assertMensaje(HttpStatus.BAD_REQUEST, "Adjunta un archivo PDF, PNG o JPG", archivo(contenido));
        }
    }

    @Test
    void unErrorAlLeerOEscribirResponde500() throws IOException {
        MultipartFile roto = mock(MultipartFile.class);
        when(roto.isEmpty()).thenReturn(false);
        when(roto.getSize()).thenReturn(10L);
        when(roto.getBytes()).thenThrow(new IOException("disco"));
        assertMensaje(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el documento", roto);

        // La carpeta de destino es un archivo: no se puede crear el directorio.
        Path ocupado = carpeta.resolve("ocupado");
        Files.writeString(ocupado, "no soy carpeta");
        AlmacenDocumentoService bloqueado = new AlmacenDocumentoService(ocupado.toString());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bloqueado.guardar(archivo(PDF)));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }

    @Test
    void leerSoloAceptaNombresGeneradosDentroDeLaCarpeta() {
        String nombre = almacen.guardar(archivo(PDF));
        assertNoEncontrado("../" + nombre);
        assertNoEncontrado("documento.pdf");
        assertNoEncontrado(nombre.replace(".pdf", ".exe"));
        ResponseStatusException ausente = assertNoEncontrado("00000000-0000-0000-0000-000000000000.pdf");
        assertEquals("Documento no disponible", ausente.getReason());
    }

    @Test
    void borrarEliminaElArchivoEIgnoraLosErrores() throws IOException {
        String nombre = almacen.guardar(archivo(PDF));
        Path guardado = carpeta.resolve("documentos").resolve(nombre);
        assertTrue(Files.exists(guardado));

        almacen.borrar(nombre);
        assertFalse(Files.exists(guardado));
        assertDoesNotThrow(() -> almacen.borrar(nombre));

        // Una carpeta con contenido no se puede borrar: el error se ignora y la carpeta queda.
        Path carpetaLlena = carpeta.resolve("documentos").resolve("llena");
        Files.createDirectories(carpetaLlena);
        Files.writeString(carpetaLlena.resolve("dentro.txt"), "x");
        assertDoesNotThrow(() -> almacen.borrar("llena"));
        assertTrue(Files.exists(carpetaLlena));
    }

    private ResponseStatusException assertNoEncontrado(String nombre) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> almacen.leer(nombre));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        return ex;
    }

    private void assertMensaje(HttpStatus estado, String mensaje, MultipartFile archivo) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> almacen.guardar(archivo));
        assertEquals(estado, ex.getStatusCode());
        assertEquals(mensaje, ex.getReason());
    }

    private static MockMultipartFile archivo(byte[] contenido) {
        return new MockMultipartFile("archivo", "subida.bin", "application/octet-stream", contenido);
    }
}
