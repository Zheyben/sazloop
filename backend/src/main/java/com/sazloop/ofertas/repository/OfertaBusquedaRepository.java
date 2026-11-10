package com.sazloop.ofertas.repository;

import com.sazloop.identidad.domain.Establecimiento;
import com.sazloop.ofertas.domain.OfertaPack;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Búsqueda pública de ofertas: la base de datos aplica los filtros (en vez de traer todas las ofertas
 * y filtrar en memoria). Una sola consulta trae oferta, pack, local y dirección con «fetch join»;
 * las colecciones (variantes, alérgenos, restricciones) se cargan por lotes (default_batch_fetch_size),
 * y las calificaciones de todos los locales encontrados salen de una sola consulta agrupada.
 * Así el número de consultas no depende de cuántas ofertas haya.
 */
@Repository
public class OfertaBusquedaRepository {
    /** Carácter de escape para los comodines de LIKE escritos por el usuario (%, _). */
    private static final char ESCAPE = '!';

    @PersistenceContext
    private EntityManager em;

    /** Suma y cantidad de las reseñas visibles de un local. */
    public record Calificacion(long suma, long cantidad) {
        public Double promedio() { return cantidad == 0 ? null : (double) suma / cantidad; }
    }

    /**
     * Ofertas activas y vigentes de locales activos, con al menos una variante con stock (y con precio
     * menor o igual al máximo, si se pide), filtradas por distrito, dirección y calificación mínima.
     * Orden: la más recientemente publicada primero.
     */
    public List<OfertaPack> buscar(String distrito, String direccion, BigDecimal precioMaximo,
                                   Double calificacionMinima, LocalDateTime ahora) {
        StringBuilder jpql = new StringBuilder(
                "select o from OfertaPack o join fetch o.packSorpresa p join fetch p.establecimiento e "
                + "join fetch e.direccion d where o.estado = :estado and o.finRecojo > :ahora "
                + "and e.estado = :activo "
                + "and exists (select 1 from VariantePack v where v.oferta = o and v.stockDisponible > 0");
        if (precioMaximo != null) jpql.append(" and v.precioDescuento <= :precio");
        jpql.append(")");
        String patronDistrito = patron(distrito);
        String patronDireccion = patron(direccion);
        if (patronDistrito != null) jpql.append(" and lower(d.distrito) like :distrito escape '").append(ESCAPE).append("'");
        if (patronDireccion != null) jpql.append(" and lower(d.direccionLinea) like :direccion escape '").append(ESCAPE).append("'");
        if (calificacionMinima != null) {
            // Promedio >= mínimo, sin dividir: suma >= mínimo × cantidad. Un local sin reseñas no cumple.
            // El margen de 0,001 evita descartar de más por redondeo; el valor exacto se revisa después.
            jpql.append(" and (select sum(r.calificacion) from Resena r where r.establecimiento = e and r.visible = true)"
                    + " >= cast(:califMinima as Double) * (select count(r2) from Resena r2 where r2.establecimiento = e and r2.visible = true)");
        }
        jpql.append(" order by o.fechaPublicacion desc, o.id desc");

        TypedQuery<OfertaPack> consulta = em.createQuery(jpql.toString(), OfertaPack.class)
                .setParameter("estado", OfertaPack.EstadoOferta.ACTIVA)
                .setParameter("ahora", ahora)
                .setParameter("activo", Establecimiento.EstadoEstablecimiento.ACTIVO);
        if (precioMaximo != null) consulta.setParameter("precio", precioMaximo);
        if (patronDistrito != null) consulta.setParameter("distrito", patronDistrito);
        if (patronDireccion != null) consulta.setParameter("direccion", patronDireccion);
        if (calificacionMinima != null) consulta.setParameter("califMinima", calificacionMinima - 0.001);
        return consulta.getResultList();
    }

    /** Calificación de varios locales en una sola consulta (los que no tienen reseñas no aparecen). */
    public Map<Long, Calificacion> calificaciones(List<Long> localIds) {
        Map<Long, Calificacion> salida = new HashMap<>();
        if (localIds.isEmpty()) return salida;
        List<Object[]> filas = em.createQuery(
                "select r.establecimiento.id, sum(r.calificacion), count(r) from Resena r "
                + "where r.visible = true and r.establecimiento.id in :ids group by r.establecimiento.id",
                Object[].class).setParameter("ids", localIds).getResultList();
        for (Object[] fila : filas)
            salida.put((Long) fila[0], new Calificacion(((Number) fila[1]).longValue(), ((Number) fila[2]).longValue()));
        return salida;
    }

    /** «%texto%» en minúsculas, con los comodines del usuario escapados; null si no hay texto. */
    private static String patron(String filtro) {
        if (filtro == null || filtro.isBlank()) return null;
        String limpio = filtro.trim().toLowerCase(Locale.ROOT)
                .replace(String.valueOf(ESCAPE), ESCAPE + String.valueOf(ESCAPE))
                .replace("%", ESCAPE + "%").replace("_", ESCAPE + "_");
        return "%" + limpio + "%";
    }
}
