package com.sazloop.ofertas.service;

import com.sazloop.experiencia.repository.ResenaRepository;
import com.sazloop.identidad.domain.CuentaEstablecimiento;
import com.sazloop.identidad.domain.Establecimiento;
import com.sazloop.identidad.repository.EstablecimientoRepository;
import com.sazloop.ofertas.domain.*;
import com.sazloop.ofertas.dto.*;
import com.sazloop.ofertas.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Publica una oferta por local y presenta la ficha pública (RF-11 a RF-15). */
@Service
public class OfertaService {
    private final PackSorpresaRepository packs;
    private final OfertaPackRepository ofertas;
    private final EstablecimientoRepository establecimientos;
    private final AlergenoRepository alergenos;
    private final RestriccionRepository restricciones;
    private final ResenaRepository resenas;
    private final OfertaBusquedaRepository busqueda;

    public OfertaService(PackSorpresaRepository packs, OfertaPackRepository ofertas,
            EstablecimientoRepository establecimientos, AlergenoRepository alergenos,
            RestriccionRepository restricciones, ResenaRepository resenas, OfertaBusquedaRepository busqueda) {
        this.busqueda = busqueda;
        this.packs = packs;
        this.ofertas = ofertas;
        this.establecimientos = establecimientos;
        this.alergenos = alergenos;
        this.restricciones = restricciones;
        this.resenas = resenas;
    }

    public record PackDisponible(Long id, String nombre) {}

    @Transactional(readOnly = true)
    public List<PackDisponible> listarPacks(CuentaEstablecimiento cuenta) {
        return packs.findByEstablecimiento_IdOrderByIdDesc(cuenta.getEstablecimiento().getId()).stream()
                .filter(PackSorpresa::isActivo)
                .map(pack -> new PackDisponible(pack.getId(), pack.getNombre()))
                .toList();
    }

    @Transactional
    public Long crearPack(CuentaEstablecimiento cuenta, PackRequest entrada) {
        Establecimiento local = establecimientos.findById(cuenta.getEstablecimiento().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PackSorpresa pack = PackSorpresa.builder().establecimiento(local)
                .nombre(entrada.nombre().trim()).descripcion(entrada.descripcion().trim())
                .posiblesProductos(entrada.posiblesProductos().trim()).build();
        for (String nombre : nombres(entrada.alergenos(), 80)) {
            pack.getAlergenos().add(alergenos.findByNombreIgnoreCase(nombre)
                    .orElseGet(() -> alergenos.save(Alergeno.builder().nombre(nombre).build())));
        }
        for (String nombre : nombres(entrada.restricciones(), 100)) {
            pack.getRestricciones().add(restricciones.findByNombreIgnoreCase(nombre)
                    .orElseGet(() -> restricciones.save(RestriccionAlimentaria.builder().nombre(nombre).build())));
        }
        return packs.save(pack).getId();
    }

    @Transactional
    public OfertaResponse crearYPublicar(CuentaEstablecimiento cuenta, PublicacionRequest entrada) {
        Long packId = crearPack(cuenta, entrada.pack());
        return publicar(cuenta, new OfertaRequest(packId, entrada.inicioRecojo(),
                entrada.finRecojo(), entrada.variantes()));
    }

    @Transactional
    public OfertaResponse publicar(CuentaEstablecimiento cuenta, OfertaRequest entrada) {
        Long localId = cuenta.getEstablecimiento().getId();
        Establecimiento local = establecimientos.bloquear(localId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (local.getEstado() == Establecimiento.EstadoEstablecimiento.SUSPENDIDO)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El local está suspendido por documentos vencidos y no puede publicar ofertas");
        if (local.getEstado() != Establecimiento.EstadoEstablecimiento.ACTIVO)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Local no aprobado");
        PackSorpresa pack = packs.findById(entrada.packId())
                .filter(p -> p.isActivo() && p.getEstablecimiento().getId().equals(localId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pack no encontrado"));
        LocalDateTime ahora = LocalDateTime.now();
        // Se tolera un inicio "ahora" (hasta 5 minutos atrás por el reloj del navegador).
        if (entrada.inicioRecojo().isBefore(ahora.minusMinutes(5)) ||
                !entrada.finRecojo().isAfter(entrada.inicioRecojo()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa las horas de recojo");
        for (OfertaPack anterior : ofertas.findByPackSorpresa_Establecimiento_IdAndEstado(
                localId, OfertaPack.EstadoOferta.ACTIVA)) {
            if (!anterior.getFinRecojo().isAfter(ahora)) anterior.setEstado(OfertaPack.EstadoOferta.FINALIZADA);
            else throw new ResponseStatusException(HttpStatus.CONFLICT, "El local ya tiene una oferta activa");
        }
        Set<VariantePack.TamanioVariante> tamanios = new HashSet<>();
        OfertaPack oferta = OfertaPack.builder().packSorpresa(pack)
                .fechaOferta(entrada.inicioRecojo().toLocalDate())
                .inicioRecojo(entrada.inicioRecojo()).finRecojo(entrada.finRecojo())
                .limiteDefinitivo(entrada.finRecojo()).estado(OfertaPack.EstadoOferta.ACTIVA).build();
        for (OfertaRequest.VarianteRequest v : entrada.variantes()) {
            if (!tamanios.add(v.tamanio()) || v.stock() < 1 ||
                    v.precioDescuento().compareTo(v.precioReferencial()) >= 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variante repetida o precio/stock inválido");
            oferta.getVariantes().add(VariantePack.builder().oferta(oferta)
                    .tamanio(v.tamanio()).precioReferencial(v.precioReferencial())
                    .precioDescuento(v.precioDescuento()).stockInicial(v.stock())
                    .stockDisponible(v.stock()).build());
        }
        return respuesta(ofertas.saveAndFlush(oferta));
    }

    @Transactional(readOnly = true)
    public Optional<OfertaResponse> vigente(CuentaEstablecimiento cuenta) {
        return ofertas.findByPackSorpresa_Establecimiento_IdAndEstado(
                        cuenta.getEstablecimiento().getId(), OfertaPack.EstadoOferta.ACTIVA).stream()
                .filter(oferta -> oferta.getFinRecojo().isAfter(LocalDateTime.now()))
                .findFirst().map(this::respuesta);
    }

    /** Búsqueda sin ubicación (sin «Cerca de mí»). */
    @Transactional(readOnly = true)
    public List<OfertaResponse> buscar(String distrito, String direccion, BigDecimal precioMaximo,
                                        Double calificacionMinima) {
        return buscar(distrito, direccion, precioMaximo, calificacionMinima, null, null);
    }

    /**
     * Búsqueda pública: la base de datos aplica los filtros (ver OfertaBusquedaRepository). Si llegan lat y lng
     * («Cerca de mí»), se calcula la distancia en línea recta (Haversine) y se ordena del más cercano al más
     * lejano; los locales sin coordenadas quedan al final. La ubicación del consumidor no se guarda.
     */
    @Transactional(readOnly = true)
    public List<OfertaResponse> buscar(String distrito, String direccion, BigDecimal precioMaximo,
                                        Double calificacionMinima, Double lat, Double lng) {
        if (precioMaximo != null && precioMaximo.signum() <= 0 ||
                calificacionMinima != null && (calificacionMinima < 0 || calificacionMinima > 5))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro inválido");
        if ((lat == null) != (lng == null) || lat != null && (!Double.isFinite(lat) || !Double.isFinite(lng)
                || lat < -90 || lat > 90 || lng < -180 || lng > 180))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ubicación inválida");
        List<OfertaPack> encontradas = busqueda.buscar(distrito, direccion, precioMaximo,
                calificacionMinima, LocalDateTime.now());
        var calificaciones = busqueda.calificaciones(encontradas.stream()
                .map(o -> o.getPackSorpresa().getEstablecimiento().getId()).distinct().toList());
        List<OfertaResponse> salida = new ArrayList<>();
        for (OfertaPack oferta : encontradas) {
            Establecimiento local = oferta.getPackSorpresa().getEstablecimiento();
            var calificacion = calificaciones.get(local.getId());
            Double promedio = calificacion == null ? null : calificacion.promedio();
            // El filtro de la base de datos tiene un pequeño margen; aquí se aplica el valor exacto.
            if (calificacionMinima != null && (promedio == null || promedio < calificacionMinima)) continue;
            salida.add(respuesta(oferta, promedio, distanciaKm(local, lat, lng)));
        }
        if (lat != null) {
            // Orden estable: los empates y los locales sin coordenadas conservan el orden por fecha de publicación.
            salida.sort(Comparator.comparing(OfertaResponse::distanciaKm, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return salida;
    }

    /** Distancia de Haversine en km, redondeada a 2 decimales; null si falta alguna de las dos ubicaciones. */
    static Double distanciaKm(Establecimiento local, Double lat, Double lng) {
        var dir = local.getDireccion();
        if (lat == null || lng == null || dir.getLatitud() == null || dir.getLongitud() == null) return null;
        double radioTierraKm = 6371.0088;
        double f1 = Math.toRadians(lat), f2 = Math.toRadians(dir.getLatitud().doubleValue());
        double df = f2 - f1, dl = Math.toRadians(dir.getLongitud().doubleValue() - lng);
        double a = Math.sin(df / 2) * Math.sin(df / 2) + Math.cos(f1) * Math.cos(f2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        double km = 2 * radioTierraKm * Math.asin(Math.min(1.0, Math.sqrt(a)));
        return Math.round(km * 100.0) / 100.0;
    }

    @Transactional(readOnly = true)
    public OfertaResponse consultar(Long id) {
        OfertaPack oferta = ofertas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (oferta.getEstado() != OfertaPack.EstadoOferta.ACTIVA ||
                !oferta.getFinRecojo().isAfter(LocalDateTime.now()) ||
                oferta.getPackSorpresa().getEstablecimiento().getEstado()
                        != Establecimiento.EstadoEstablecimiento.ACTIVO)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return respuesta(oferta);
    }

    /** Ficha de una sola oferta (publicación o consulta): calcula su calificación con una consulta propia. */
    private OfertaResponse respuesta(OfertaPack oferta) {
        var calificaciones = resenas.findByEstablecimiento_IdAndVisibleTrue(oferta.getPackSorpresa().getEstablecimiento().getId());
        Double promedio = calificaciones.isEmpty() ? null : calificaciones.stream()
                .mapToInt(r -> r.getCalificacion()).average().orElseThrow();
        return respuesta(oferta, promedio, null);
    }

    private OfertaResponse respuesta(OfertaPack oferta, Double promedio, Double distanciaKm) {
        PackSorpresa pack = oferta.getPackSorpresa();
        Establecimiento local = pack.getEstablecimiento();
        return new OfertaResponse(oferta.getId(), local.getId(), local.getNombre(),
                local.getDireccion().getDistrito(), local.getDireccion().getDireccionLinea(), promedio,
                pack.getNombre(), pack.getDescripcion(), pack.getPosiblesProductos(),
                pack.getAlergenos().stream().map(Alergeno::getNombre).sorted().toList(),
                pack.getRestricciones().stream().map(RestriccionAlimentaria::getNombre).sorted().toList(),
                oferta.getInicioRecojo(), oferta.getFinRecojo(), oferta.getLimiteDefinitivo(),
                oferta.getEstado(), oferta.getVariantes().stream()
                .map(v -> new OfertaResponse.Variante(v.getId(), v.getTamanio(),
                        v.getPrecioReferencial(), v.getPrecioDescuento(),
                        v.getPrecioReferencial().subtract(v.getPrecioDescuento()),
                        v.getStockDisponible())).toList(), distanciaKm);
    }

    private static Set<String> nombres(List<String> entrada, int max) {
        Set<String> salida = new LinkedHashSet<>();
        if (entrada == null) return salida;
        for (String valor : entrada) {
            String nombre = valor.trim();
            if (nombre.isEmpty() || nombre.length() > max)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de advertencia inválido");
            salida.add(nombre);
        }
        return salida;
    }
}
