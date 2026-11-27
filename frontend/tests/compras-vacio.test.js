// «Mis compras» con el carrito vacío, historial con otros estados y fallos al cargar.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, respuesta, CONSUMIDOR } from './navegador.js';

const CLAVE = `sazloop-carrito-${CONSUMIDOR.id}`;
const BASE = {
  establecimiento: 'Aurora Miraflores', establecimientoId: 2, medioPago: 'EFECTIVO', modalidad: 'RECOJO',
  inicioRecojo: '2026-11-26T18:00:00', finRecojo: '2026-11-26T21:00:00', resenaPendiente: false,
  cancelable: false, total: 14, importeReembolso: null
};

const pagina = abrirPagina('compras.html', {
  servidor: {
    'GET /api/sesion': CONSUMIDOR,
    'GET /api/notificaciones': [],
    'GET /api/compras/mias': [
      { ...BASE, reservaId: 1, estado: 'PENDIENTE_PAGO', codigoRecojo: 'PEND-1', numeroBoleta: null },
      { ...BASE, reservaId: 2, estado: 'EXPIRADA', codigoRecojo: 'EXP-2', numeroBoleta: null },
      { ...BASE, reservaId: 3, estado: 'DESCONOCIDO', codigoRecojo: 'RARO-3', numeroBoleta: 'SIM-3' }
    ]
  }
});
await import('../app/portal-main.js');
await esperar(10);

const tarjeta = codigo => pagina.$$('#lista .tarjeta-compra').find(t => t.textContent.includes(codigo)).textContent;

/** Vuelve a abrir «Mis compras» pasando por otra vista del portal (navegación sin recargar). */
async function volverAMisCompras() {
  clic(pagina.$('a.portal-campana'));
  await esperar();
  clic(pagina.$('.portal-enlaces a[href="compras.html"]'));
  await esperar(10);
}

test('con el carrito vacío invita a explorar productos', () => {
  const carrito = pagina.$('#carrito');
  assert.match(carrito.textContent, /Tu carrito está vacío\./);
  assert.equal(carrito.querySelector('a').getAttribute('href'), 'catalogo.html');
  assert.equal(pagina.llamadas.filter(l => l.ruta.startsWith('/api/ofertas/')).length, 0);
});

test('el historial muestra pendiente de pago, expirada y estados desconocidos', () => {
  assert.match(tarjeta('PEND-1'), /Pendiente de pago.*Medio de pago: EFECTIVO.*Boleta disponible al confirmar el efectivo\./);
  assert.match(tarjeta('EXP-2'), /Expirada/);
  assert.doesNotMatch(tarjeta('EXP-2'), /Boleta/);
  assert.match(tarjeta('RARO-3'), /^DESCONOCIDO.*Boleta SIM-3/);
  assert.equal(pagina.$$('#lista button').length, 0, 'ninguna se puede cancelar ni calificar');
});

test('sin compras muestra el mensaje de historial vacío', async () => {
  pagina.servidor['GET /api/compras/mias'] = [];
  await volverAMisCompras();
  assert.match(pagina.$('#lista').textContent, /Aún no tienes compras realizadas\./);
});

test('si el historial falla, avisa que no se pudieron cargar las compras', async () => {
  pagina.alertas.length = 0;
  pagina.servidor['GET /api/compras/mias'] = respuesta(403, { message: 'Forbidden' });
  await volverAMisCompras();
  assert.equal(pagina.$('#mensaje').textContent, 'No tienes permiso para esta acción.');
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudieron cargar tus compras'));
});

test('si la oferta llega incompleta, avisa que no se pudo mostrar el carrito', async () => {
  pagina.alertas.length = 0;
  pagina.servidor['GET /api/compras/mias'] = [];
  pagina.servidor['GET /api/ofertas/30'] = { id: 30, pack: 'Pack sin variantes' };
  pagina.window.sessionStorage.setItem(CLAVE, JSON.stringify([{ clave: 'x', ofertaId: 30, lineas: [{ varianteId: 1, cantidad: 1 }] }]));
  await volverAMisCompras();
  assert.equal(pagina.llamadasA('GET', '/api/ofertas/30').length, 1);
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudo mostrar el carrito'));
  assert.match(pagina.$('#carrito').textContent, /Revisando disponibilidad/);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
