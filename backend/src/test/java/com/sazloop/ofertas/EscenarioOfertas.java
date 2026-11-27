package com.sazloop.ofertas;

import com.sazloop.identidad.domain.CuentaEstablecimiento;
import com.sazloop.identidad.domain.Establecimiento;
import com.sazloop.identidad.domain.Usuario;
import com.sazloop.identidad.dto.CuentaLocalRequest;
import com.sazloop.identidad.dto.DireccionRequest;
import com.sazloop.identidad.dto.EmpresaRequest;
import com.sazloop.identidad.dto.EstablecimientoRequest;
import com.sazloop.identidad.repository.EstablecimientoRepository;
import com.sazloop.identidad.repository.UsuarioRepository;
import com.sazloop.identidad.service.CuentaLocalService;
import com.sazloop.identidad.service.EmpresaService;
import com.sazloop.ofertas.domain.VariantePack;
import com.sazloop.ofertas.dto.OfertaRequest;
import com.sazloop.ofertas.dto.OfertaResponse;
import com.sazloop.ofertas.dto.PackRequest;
import com.sazloop.ofertas.dto.PublicacionRequest;
import com.sazloop.ofertas.service.OfertaService;
import com.sazloop.reservas.dto.CompraRequest;
import com.sazloop.reservas.dto.CompraResponse;
import com.sazloop.reservas.service.CompraRecojoService;
import com.sazloop.shared.Tiempo;
import com.sazloop.transacciones.domain.Pago;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Arma datos de prueba propios (usuario, empresa, local aprobado con su cuenta, ofertas y compras)
 * sobre la base H2 real. Cada llamada usa correos, RUC y nombres únicos para no depender de otros tests
 * ni de los datos de demostración.
 */
public final class EscenarioOfertas {
    private final UsuarioRepository usuarios;
    private final EmpresaService empresas;
    private final EstablecimientoRepository establecimientos;
    private final CuentaLocalService cuentas;
    private final OfertaService ofertas;
    private final CompraRecojoService compras;

    /** Local aprobado con su propietario y su cuenta operativa. */
    public record LocalPrueba(Long propietarioId, Long localId, CuentaEstablecimiento cuenta) {}

    public EscenarioOfertas(ApplicationContext contexto) {
        this.usuarios = contexto.getBean(UsuarioRepository.class);
        this.empresas = contexto.getBean(EmpresaService.class);
        this.establecimientos = contexto.getBean(EstablecimientoRepository.class);
        this.cuentas = contexto.getBean(CuentaLocalService.class);
        this.ofertas = contexto.getBean(OfertaService.class);
        this.compras = contexto.getBean(CompraRecojoService.class);
    }

    /** Texto único corto, útil como distrito para que la búsqueda solo encuentre lo creado por el test. */
    public static String unico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 12);
    }

    public Long usuario() {
        return usuarios.save(Usuario.builder().nombres("Prueba").apellidos("Ofertas")
                .email(UUID.randomUUID() + "@example.test").passwordHash("hash-temporal").build()).getId();
    }

    public LocalPrueba localActivo(String distrito, String direccion) {
        return localActivo(distrito, direccion, null, null);
    }

    /** Registra empresa y local, lo deja ACTIVO (atajo de la aprobación) y le crea la cuenta operativa. */
    public LocalPrueba localActivo(String distrito, String direccion, String latitud, String longitud) {
        Long propietario = usuario();
        String ruc = Long.toString(ThreadLocalRandom.current().nextLong(10_000_000_000L, 100_000_000_000L));
        empresas.registrarEmpresa(propietario, new EmpresaRequest("Empresa prueba", "Comercio prueba", ruc));
        Long local = empresas.registrarEstablecimiento(propietario, new EstablecimientoRequest(
                "Local " + distrito, null, null, new DireccionRequest("Lima", "Lima", distrito, direccion,
                        null, null, null,
                        latitud == null ? null : new BigDecimal(latitud),
                        longitud == null ? null : new BigDecimal(longitud)))).id();
        cambiarEstado(local, Establecimiento.EstadoEstablecimiento.ACTIVO);
        CuentaEstablecimiento cuenta = cuentas.crear(propietario, local,
                new CuentaLocalRequest(unico("local"), "clave-local"));
        return new LocalPrueba(propietario, local, cuenta);
    }

    public void cambiarEstado(Long localId, Establecimiento.EstadoEstablecimiento estado) {
        Establecimiento local = establecimientos.findById(localId).orElseThrow();
        local.setEstado(estado);
        establecimientos.saveAndFlush(local);
    }

    public static PackRequest pack(String nombre) {
        return new PackRequest(nombre, "Excedentes del día", "Pan y fruta", List.of("Trigo"), List.of("Vegano"));
    }

    public static OfertaRequest.VarianteRequest variante(VariantePack.TamanioVariante tamanio,
            String referencial, String descuento, int stock) {
        return new OfertaRequest.VarianteRequest(tamanio, new BigDecimal(referencial), new BigDecimal(descuento), stock);
    }

    /** Publica pack y oferta juntos; el recojo empezó hace un minuto y dura dos horas. */
    public OfertaResponse publicar(LocalPrueba local, OfertaRequest.VarianteRequest... variantes) {
        LocalDateTime inicio = Tiempo.ahora().minusMinutes(1);
        return ofertas.crearYPublicar(local.cuenta(), new PublicacionRequest(pack("Pack " + local.localId()),
                inicio, inicio.plusHours(2), List.of(variantes)));
    }

    /** Oferta típica: una variante pequeña de S/ 20 a S/ 10 con el stock indicado. */
    public OfertaResponse publicar(LocalPrueba local, int stock) {
        return publicar(local, variante(VariantePack.TamanioVariante.PEQUENO, "20.00", "10.00", stock));
    }

    public CompraResponse comprar(Long consumidor, OfertaResponse oferta) {
        return compras.comprar(consumidor, new CompraRequest(oferta.id(), Pago.MedioPago.YAPE,
                List.of(new CompraRequest.Linea(oferta.variantes().getFirst().id(), 1))));
    }

    /** Compra una unidad y el local la entrega: la reserva queda RECOGIDA. */
    public CompraResponse comprarYRecoger(Long consumidor, OfertaResponse oferta, LocalPrueba local) {
        CompraResponse compra = comprar(consumidor, oferta);
        compras.recoger(local.cuenta(), compra.codigoRecojo());
        return compra;
    }
}
