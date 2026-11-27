// Pantalla «Mis compras» del consumidor (app/compras.html + compras.js): carrito, pago, delivery,
// confirmación de la compra, historial y cancelación.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, respuesta, CONSUMIDOR } from './navegador.js';

const CLAVE = `sazloop-carrito-${CONSUMIDOR.id}`;

const OFERTA = {
  id: 10, establecimientoId: 2, establecimiento: 'Aurora Miraflores', distrito: 'Miraflores',
  direccion: 'Av. Larco 100', calificacion: 4.5, pack: 'Pack sorpresa de panadería',
  descripcion: 'Panes del día', posiblesProductos: 'Croissants, baguettes', alergenos: 'Gluten',
  restricciones: null, inicioRecojo: '2026-11-28T18:00:00', finRecojo: '2026-11-28T21:00:00',
  limiteDefinitivo: '2026-11-28T21:00:00', estado: 'PUBLICADA', distanciaKm: 1.2,
  variantes: [
    { id: 101, tamanio: 'MEDIANO', precioReferencial: 30, precioDescuento: 14, stockDisponible: 3 },
    { id: 102, tamanio: 'GRANDE', precioReferencial: 45, precioDescuento: 20, stockDisponible: 2 }
  ]
};

// Pedido 1 y 2 piden la misma variante (2 + 2 > stock 3); el 3 es de una oferta que ya no existe;
// el 4 apunta a una variante que la oferta ya no tiene.
const CARRITO = [
  { clave: 'a', ofertaId: 10, lineas: [{ varianteId: 101, cantidad: 2 }, { varianteId: 102, cantidad: 1 }] },
  { clave: 'b', ofertaId: 10, lineas: [{ varianteId: 101, cantidad: 2 }] },
  { clave: 'c', ofertaId: 20, lineas: [{ varianteId: 201, cantidad: 1 }] },
  { clave: 'd', ofertaId: 10, lineas: [{ varianteId: 999, cantidad: 1 }] }
];

const COMPRA_BASE = {
  establecimiento: 'Aurora Miraflores', establecimientoId: 2,
  inicioRecojo: '2026-11-26T18:08:28', finRecojo: '2026-11-27T02:08:28',
  lineas: [{ cantidad: 1, precioUnitario: 14, tamanio: 'MEDIANO' }], medioPago: 'YAPE',
  modalidad: 'RECOJO', pagoSimulado: true, avisoBoleta: 'Comprobante simulado sin validez tributaria',
  resenaPendiente: false, cliente: 'Valeria Q.', empresaDelivery: null, entregaDireccion: null,
  entregaDistrito: null, entregaReferencia: null, importeReembolso: null
};
const HISTORIAL = [
  { ...COMPRA_BASE, reservaId: 1, codigoRecojo: '805949F6-20C9-42', estado: 'CONFIRMADA', numeroBoleta: 'SIM-1',
    total: 14, cancelable: true },
  { ...COMPRA_BASE, reservaId: 2, codigoRecojo: 'AAAA-2', estado: 'RECOGIDA', numeroBoleta: 'SIM-2', total: 20,
    medioPago: 'TARJETA_DEBITO', cancelable: false, inicioRecojo: '2026-11-25T18:00:00', finRecojo: '2026-11-25T21:00:00' },
  { ...COMPRA_BASE, reservaId: 3, codigoRecojo: 'BBBB-3', estado: 'CANCELADA', numeroBoleta: 'SIM-3', total: 30,
    importeReembolso: 15, cancelable: false },
  { ...COMPRA_BASE, reservaId: 4, codigoRecojo: 'CCCC-4', estado: 'CONFIRMADA', numeroBoleta: 'SIM-4', total: 18,
    cancelable: false, modalidad: 'DELIVERY', empresaDelivery: 'RAPPI', entregaDireccion: 'Av. Pardo 200',
    entregaDistrito: 'Miraflores', entregaReferencia: 'Frente al parque' },
  { ...COMPRA_BASE, reservaId: 5, codigoRecojo: 'DDDD-5', estado: 'RECOGIDA', numeroBoleta: 'SIM-5', total: 18,
    cancelable: false, modalidad: 'DELIVERY', empresaDelivery: 'PEDIDOSYA', entregaDireccion: 'Calle 1',
    entregaDistrito: 'Surco', entregaReferencia: null }
];

let historial = structuredClone(HISTORIAL);
const pagina = abrirPagina('compras.html', {
  servidor: {
    'GET /api/sesion': CONSUMIDOR,
    'GET /api/ofertas/10': () => structuredClone(OFERTA),
    'GET /api/ofertas/20': respuesta(404, { message: 'Oferta no encontrada' }),
    'GET /api/compras/mias': () => historial,
    'GET /api/notificaciones': []
  },
  confirmar: false
});
pagina.window.sessionStorage.setItem(CLAVE, JSON.stringify(CARRITO));
await import('../app/portal-main.js');
await esperar(10);

// ---- Ayudas locales ----
const pedidosDisponibles = () => pagina.$$('.carrito-pedido:not(.no-disponible)');
const cantidades = pedido => [...pedido.querySelectorAll('input[type="number"]')];
const boton = (texto, raiz = pagina.document) => [...raiz.querySelectorAll('button')].find(b => b.textContent.trim() === texto);
const grupo = leyenda => pagina.$$('fieldset').find(f => f.querySelector('legend')?.textContent === leyenda);
const mensajeError = () => [...pagina.$('.carrito-pago-panel').querySelectorAll('p[role="status"]')].at(-1).textContent;
const mensajePago = () => pagina.$('.carrito-pago-panel').querySelectorAll('p[role="status"]')[0].textContent;
const confirmarCompra = () => boton('Confirmar compra');
const carritoGuardado = () => JSON.parse(pagina.window.sessionStorage.getItem(CLAVE) || 'null');
function escribir(campo, valor) {
  campo.value = valor;
  campo.dispatchEvent(new pagina.window.Event('input', { bubbles: true }));
  campo.dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
}
function enviar() { pagina.$('.carrito-formulario').requestSubmit(); }

test('muestra los pedidos del carrito y marca los que ya no están disponibles', () => {
  assert.equal(pagina.$$('.carrito-pedido').length, 4);
  assert.equal(pedidosDisponibles().length, 2);
  const noDisponibles = pagina.$$('.carrito-pedido.no-disponible').map(a => a.textContent);
  assert.match(noDisponibles[0], /PEDIDO 3.*Esta oferta ya no está disponible/);
  assert.match(noDisponibles[1], /PEDIDO 4.*Las variantes de este pedido ya no están disponibles/);
  assert.match(pedidosDisponibles()[0].textContent, /Pack sorpresa de panadería/);
  assert.match(pedidosDisponibles()[0].textContent, /Aurora Miraflores · Miraflores/);
  assert.match(pedidosDisponibles()[0].textContent, /Mediano.*S\/ 14\.00.*S\/ 30\.00.*Quedan 3/);
  assert.equal(pedidosDisponibles()[0].querySelector('.carrito-subtotal').textContent, 'S/ 48.00');
  assert.equal(pedidosDisponibles()[1].querySelector('.carrito-subtotal').textContent, 'S/ 28.00');
});

test('avisa que la suma de pedidos supera el stock y no deja confirmar', () => {
  assert.match(pagina.texto(), /La suma de los pedidos supera el stock disponible\./);
  assert.equal(confirmarCompra().disabled, true);
});

test('el medio de pago empieza en Yape y la tarjeta está oculta', () => {
  assert.equal(grupo('Yape').hidden, false);
  assert.equal(grupo('Tarjeta de débito').hidden, true);
  assert.equal(pagina.$('select[name="medioPago"]').value, 'YAPE');
  const yape = pagina.$$('.carrito-metodo').find(b => b.dataset.valor === 'YAPE');
  assert.equal(yape.getAttribute('aria-checked'), 'true');
});

test('quitar un pedido no disponible lo saca del carrito y vuelve a dibujarlo', async () => {
  const quitar = boton('Quitar del carrito', pagina.$$('.carrito-pedido')[2]);
  clic(quitar);
  await esperar();
  assert.deepEqual(carritoGuardado().map(p => p.clave), ['a', 'b', 'd']);
  assert.equal(pagina.$$('.carrito-pedido').length, 3);
  assert.doesNotMatch(pagina.texto(), /Esta oferta ya no está disponible/);

  clic(boton('Quitar del carrito', pagina.$$('.carrito-pedido.no-disponible')[0]));
  await esperar();
  assert.deepEqual(carritoGuardado().map(p => p.clave), ['a', 'b']);
  assert.equal(pagina.$$('.carrito-pedido.no-disponible').length, 0);
  assert.equal(pagina.$('#contador-carrito').textContent, '2');
});

test('el botón «−» baja la cantidad, recalcula el total y guarda el carrito', () => {
  const segundo = pedidosDisponibles()[1];
  clic(segundo.querySelector('.detalle-menos'));
  assert.equal(cantidades(segundo)[0].value, '1');
  assert.equal(segundo.querySelector('.carrito-subtotal').textContent, 'S/ 14.00');
  assert.equal(pagina.$('.carrito-total').textContent, 'S/ 62.00');
  assert.deepEqual(carritoGuardado()[1].lineas, [{ varianteId: 101, cantidad: 1 }]);
  assert.ok(pedidosDisponibles()[0].querySelector('.detalle-variante').classList.contains('seleccionada'));
});

test('el botón «+» no pasa del stock disponible', () => {
  const primero = pedidosDisponibles()[0];
  const grande = cantidades(primero)[1];
  const mas = primero.querySelectorAll('.detalle-mas')[1];
  clic(mas);
  clic(mas);
  clic(mas);
  assert.equal(grande.value, '2', 'el stock de la variante grande es 2');
  assert.equal(pagina.$('.carrito-total').textContent, 'S/ 82.00');
  clic(primero.querySelectorAll('.detalle-menos')[1]);
  assert.equal(grande.value, '1');
  assert.equal(pagina.$('.carrito-total').textContent, 'S/ 62.00');
});

test('una cantidad no válida pide revisar las cantidades', () => {
  const entrada = cantidades(pedidosDisponibles()[0])[0];
  escribir(entrada, '2.5');
  assert.match(pagina.texto(), /Revisa las cantidades de los pedidos\./);
  escribir(entrada, '2');
  assert.doesNotMatch(pagina.texto(), /Revisa las cantidades/);
});

test('Yape: un número que no empieza por 9 muestra un aviso de error', () => {
  pagina.alertas.length = 0;
  const [telefono] = grupo('Yape').querySelectorAll('input');
  escribir(telefono, '812345678');
  clic(boton('Siguiente'));
  assert.equal(mensajePago(), 'Ingresa un número peruano de nueve dígitos que empiece por 9.');
  assert.equal(pagina.ultimaAlerta().title, 'Revisa el pago');
  assert.equal(pagina.ultimaAlerta().icon, 'warning');
});

test('Yape: con número válido pide el código y lo valida', () => {
  const [telefono, codigo] = grupo('Yape').querySelectorAll('input');
  escribir(telefono, '987654321');
  clic(boton('Siguiente'));
  assert.equal(codigo.disabled, false);
  assert.equal(mensajePago(), '');

  escribir(codigo, '12');
  clic(boton('Validar', grupo('Yape')));
  assert.equal(mensajePago(), 'Ingresa los seis dígitos del código.');

  escribir(codigo, '123456');
  clic(boton('Validar', grupo('Yape')));
  assert.equal(mensajePago(), 'Yape validado. Acepta los términos para continuar.');
  assert.equal(pagina.ultimaAlerta().toast, true);
  assert.equal(telefono.readOnly, true);
  assert.equal(confirmarCompra().disabled, true, 'falta aceptar los términos');
});

test('aceptar los términos habilita «Confirmar compra»', () => {
  const terminos = pagina.$('.carrito-terminos input');
  assert.equal(terminos.disabled, false);
  terminos.checked = true;
  terminos.dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
  assert.equal(confirmarCompra().disabled, false);
});

test('el delivery exige empresa, distrito y dirección', () => {
  const delivery = pagina.$$('.carrito-metodo').find(b => b.dataset.valor === 'DELIVERY');
  clic(delivery);
  const panel = pagina.$('.carrito-delivery');
  assert.equal(panel.hidden, false);
  assert.equal(delivery.getAttribute('aria-checked'), 'true');
  assert.equal(confirmarCompra().disabled, true);
  assert.equal(mensajePago(), 'Completa los datos del delivery para poder pagar.');

  clic(pagina.$$('.empresa-delivery').find(b => b.dataset.valor === 'RAPPI'));
  escribir(pagina.$('input[placeholder="Ej.: Miraflores"]'), 'Miraflores');
  assert.equal(confirmarCompra().disabled, true, 'aún falta la dirección');
  escribir(pagina.$('input[placeholder="Ej.: Av. Larco 1280, depto. 302"]'), 'Av. Larco 1280');
  assert.equal(confirmarCompra().disabled, false);
  assert.equal(mensajePago(), '');
  assert.equal(pagina.$$('.empresa-delivery').find(b => b.dataset.valor === 'RAPPI').getAttribute('aria-checked'), 'true');
});

test('volver a recojo en el local oculta el panel de delivery', () => {
  clic(pagina.$$('.carrito-metodo').find(b => b.dataset.valor === 'RECOJO'));
  assert.equal(pagina.$('.carrito-delivery').hidden, true);
  assert.equal(confirmarCompra().disabled, false);
});

test('si el servidor rechaza la compra, muestra el motivo y deja reintentar', async () => {
  pagina.alertas.length = 0;
  pagina.servidor['POST /api/compras/lote'] = respuesta(409, { message: 'Ya no queda stock del pack' });
  enviar();
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/compras/lote').length, 1);
  assert.equal(mensajeError(), 'Ya no queda stock del pack');
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudo confirmar la compra' && a.text === 'Ya no queda stock del pack'));
  assert.equal(confirmarCompra().disabled, false);
  assert.equal(carritoGuardado().length, 2, 'el carrito se conserva');
});

test('sin conexión avisa que revise su internet', async () => {
  pagina.alertas.length = 0;
  pagina.servidor['POST /api/compras/lote'] = new TypeError('Failed to fetch');
  enviar();
  await esperar();
  assert.equal(mensajeError(), 'Sin conexión. Revisa tu internet e intenta de nuevo.');
  assert.ok(pagina.alertas.some(a => a.title === 'Sin conexión'));
});

test('con todas las cantidades en cero no deja confirmar', async () => {
  pagina.alertas.length = 0;
  const llamadas = pagina.llamadasA('POST', '/api/compras/lote').length;
  for (const entrada of pedidosDisponibles().flatMap(cantidades)) entrada.value = '0';
  enviar();
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/compras/lote').length, llamadas);
  const alerta = pagina.alertas.find(a => a.title === 'No se puede confirmar la compra');
  assert.equal(alerta?.text, 'Elige al menos una unidad.');
});

test('cambiar a tarjeta reinicia el pago y valida los datos de la tarjeta', () => {
  escribir(cantidades(pedidosDisponibles()[0])[0], '2');
  escribir(cantidades(pedidosDisponibles()[0])[1], '1');
  escribir(cantidades(pedidosDisponibles()[1])[0], '1');
  const tarjetaBoton = pagina.$$('.carrito-metodos[aria-label] .carrito-metodo')
    .find(b => b.dataset.valor === 'TARJETA_DEBITO');
  clic(tarjetaBoton);
  clic(tarjetaBoton); // un segundo clic sobre el mismo medio no hace nada
  assert.equal(grupo('Tarjeta de débito').hidden, false);
  assert.equal(grupo('Yape').hidden, true);
  assert.equal(confirmarCompra().disabled, true);
  assert.equal(pagina.$('.carrito-terminos input').checked, false);

  const [numero, titular, vencimiento, cvv] = grupo('Tarjeta de débito').querySelectorAll('input');
  escribir(numero, '4111 1111 1111 1111');
  escribir(titular, 'Valeria Quispe');
  escribir(vencimiento, '2020-01');
  escribir(cvv, '123');
  clic(boton('Validar', grupo('Tarjeta de débito')));
  assert.equal(mensajePago(), 'Revisa el número, titular, vencimiento y CVV o CVC.');
  assert.equal(numero.readOnly, false);

  escribir(vencimiento, '2030-12');
  clic(boton('Validar', grupo('Tarjeta de débito')));
  assert.equal(mensajePago(), 'Tarjeta validada. Acepta los términos para continuar.');
  assert.equal(numero.readOnly, true);
});

test('confirmar la compra con delivery envía el pedido y muestra las boletas', async () => {
  pagina.alertas.length = 0;
  const terminos = pagina.$('.carrito-terminos input');
  terminos.checked = true;
  terminos.dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
  clic(pagina.$$('.carrito-metodo').find(b => b.dataset.valor === 'DELIVERY'));
  assert.equal(confirmarCompra().disabled, false, 'la empresa y la dirección ya estaban llenas');
  escribir(pagina.$('input[placeholder="Ej.: Frente al parque (opcional)"]'), '  Frente al parque  ');

  const nuevas = [
    { ...COMPRA_BASE, reservaId: 6, codigoRecojo: 'NUEVO-6', estado: 'CONFIRMADA', numeroBoleta: 'SIM-6', total: 48,
      cancelable: true, modalidad: 'DELIVERY', empresaDelivery: 'RAPPI', entregaDireccion: 'Av. Larco 1280',
      entregaDistrito: 'Miraflores', entregaReferencia: 'Frente al parque',
      inicioRecojo: '2026-11-28T18:00:00', finRecojo: '2026-11-28T21:00:00' },
    { ...COMPRA_BASE, reservaId: 7, codigoRecojo: 'NUEVO-7', estado: 'CONFIRMADA', numeroBoleta: 'SIM-7', total: 14,
      cancelable: true }
  ];
  pagina.servidor['POST /api/compras/lote'] = () => { historial = [...nuevas, ...historial]; return nuevas; };
  const consultasHistorial = pagina.llamadasA('GET', '/api/compras/mias').length;
  enviar();
  await esperar();

  const [llamada] = pagina.llamadasA('POST', '/api/compras/lote').slice(-1);
  assert.deepEqual(llamada.cuerpo, {
    medioPago: 'TARJETA_DEBITO',
    pedidos: [
      { ofertaId: 10, lineas: [{ varianteId: 101, cantidad: 2 }, { varianteId: 102, cantidad: 1 }] },
      { ofertaId: 10, lineas: [{ varianteId: 101, cantidad: 1 }] }
    ],
    modalidad: 'DELIVERY', empresaDelivery: 'RAPPI', entregaDistrito: 'Miraflores',
    entregaDireccion: 'Av. Larco 1280', entregaReferencia: 'Frente al parque'
  });
  const confirmada = pagina.$('.compra-confirmada').textContent;
  assert.match(confirmada, /Compra confirmada/);
  assert.match(confirmada, /Total: S\/ 62\.00/);
  assert.match(confirmada, /Pedido 1: Aurora Miraflores.*Boleta SIM-6.*Código de recojo: NUEVO-6/);
  assert.match(confirmada, /Modalidad: Delivery con Rappi – Av\. Larco 1280, Miraflores \(Frente al parque\)/);
  assert.match(confirmada, /El local lo entrega al repartidor: /);
  assert.match(confirmada, /Modalidad: Recojo en el local/);
  assert.equal(carritoGuardado(), null, 'el carrito queda vacío');
  assert.ok(pagina.alertas.some(a => a.title === 'Compra confirmada' && a.icon === 'success'));
  assert.equal(pagina.llamadasA('GET', '/api/compras/mias').length, consultasHistorial + 1);
  assert.match(pagina.$('#lista').textContent, /NUEVO-6/);
});

test('el historial muestra cada estado con su etiqueta', () => {
  const tarjetas = pagina.$$('#lista .tarjeta-compra');
  const texto = codigo => tarjetas.find(t => t.textContent.includes(codigo)).textContent;
  assert.match(texto('805949F6-20C9-42'), /Lista para recoger.*S\/ 14\.00.*Medio de pago: YAPE.*Recojo en el local.*Boleta SIM-1/);
  assert.match(texto('AAAA-2'), /Recogida.*Medio de pago: Tarjeta de débito/);
  assert.match(texto('AAAA-2'), /25 nov\.? 2026 · 18:00 – 21:00/);
  assert.match(texto('CCCC-4'), /Esperando al repartidor.*Delivery · Rappi – Av\. Pardo 200, Miraflores \(Frente al parque\)/);
  assert.match(texto('DDDD-5'), /Entregado al repartidor.*PedidosYa – Calle 1, Surco/);
  const cancelada = tarjetas.find(t => t.classList.contains('estado-cancelada'));
  assert.match(cancelada.textContent, /Cancelada.*Reembolso: S\/ 15\.00/);
  assert.doesNotMatch(cancelada.textContent, /BBBB-3/, 'una compra cancelada no muestra el código');
  assert.equal(tarjetas.filter(t => boton('Cancelar compra', t)).length, 3);
});

test('si la persona no confirma, la compra no se cancela', async () => {
  pagina.alertas.length = 0;
  pagina.confirmar = false;
  const tarjeta = pagina.$$('#lista .tarjeta-compra').find(t => t.textContent.includes('805949F6'));
  clic(boton('Cancelar compra', tarjeta));
  await esperar();
  const pregunta = pagina.alertas.find(a => a.title === '¿Cancelar esta compra?');
  assert.match(pregunta.text, /Recibirás S\/ 7\.00 de reembolso \(50 % del total\)/);
  assert.equal(pagina.llamadas.filter(l => l.metodo === 'POST' && /\/cancelar$/.test(l.ruta)).length, 0);
});

test('al confirmar se cancela, se avisa el reembolso y se recarga el historial', async () => {
  pagina.alertas.length = 0;
  pagina.confirmar = true;
  pagina.servidor['POST /api/compras/1/cancelar'] = () => {
    historial = historial.map(c => c.reservaId === 1 ? { ...c, estado: 'CANCELADA', cancelable: false, importeReembolso: 7 } : c);
    return respuesta(204);
  };
  const tarjeta = pagina.$$('#lista .tarjeta-compra').find(t => t.textContent.includes('805949F6'));
  clic(boton('Cancelar compra', tarjeta));
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/compras/1/cancelar').length, 1);
  const exito = pagina.alertas.find(a => a.title === 'Compra cancelada');
  assert.equal(exito.text, 'El reembolso de S/ 7.00 quedó registrado.');
  const cancelada = pagina.$$('#lista .tarjeta-compra').find(t => t.textContent.includes('SIM-1'));
  assert.match(cancelada.textContent, /Cancelada.*Reembolso: S\/ 7\.00/);
});

test('si cancelar falla, muestra el motivo del servidor', async () => {
  pagina.alertas.length = 0;
  pagina.servidor['POST /api/compras/7/cancelar'] = respuesta(409, { message: 'El horario de recojo ya terminó' });
  const tarjeta = pagina.$$('#lista .tarjeta-compra').find(t => t.textContent.includes('NUEVO-7'));
  clic(boton('Cancelar compra', tarjeta));
  await esperar();
  assert.equal(pagina.$('#mensaje').textContent, 'El horario de recojo ya terminó');
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudo cancelar la compra'));
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
