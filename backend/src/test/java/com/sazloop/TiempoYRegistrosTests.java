package com.sazloop;

import com.sazloop.afiliacion.service.SolicitudAfiliacionService.ArchivoPrivado;
import com.sazloop.identidad.service.PerfilService.Foto;
import com.sazloop.shared.Tiempo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas sin Spring: el reloj único del sistema y los registros que contienen arreglos de bytes. */
class TiempoYRegistrosTests {

    @Test
    void elRelojUsaLaHoraDePeruSinImportarLaZonaDelServidor() {
        assertEquals(ZoneId.of("America/Lima"), Tiempo.ZONA);
        ZonedDateTime esperado = ZonedDateTime.now(ZoneId.of("America/Lima"));
        ZonedDateTime delSistema = Tiempo.ahora().atZone(Tiempo.ZONA);
        assertTrue(Duration.between(delSistema, esperado).abs().getSeconds() < 5);
        assertEquals(esperado.toLocalDate(), Tiempo.hoy());
    }

    @Test
    void losRegistrosConArreglosSeComparanPorContenido() {
        ArchivoPrivado a = new ArchivoPrivado(new byte[] {1, 2, 3}, "pdf");
        ArchivoPrivado b = new ArchivoPrivado(new byte[] {1, 2, 3}, "pdf");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new ArchivoPrivado(new byte[] {1, 2, 4}, "pdf"));
        assertNotEquals(a, new ArchivoPrivado(new byte[] {1, 2, 3}, "png"));
        assertTrue(a.toString().contains("3 bytes"));

        Foto f = new Foto("image/png", new byte[] {9, 8});
        assertEquals(f, new Foto("image/png", new byte[] {9, 8}));
        assertEquals(f.hashCode(), new Foto("image/png", new byte[] {9, 8}).hashCode());
        assertNotEquals(f, new Foto("image/jpeg", new byte[] {9, 8}));
        assertNotEquals(f, new Foto("image/png", new byte[] {9}));
        assertTrue(f.toString().contains("2 bytes"));
    }
}
