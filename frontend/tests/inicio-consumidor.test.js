// Inicio del consumidor (app/consumidor.html + consumidor.js): saludo y «Novedades» (máximo 3 packs),
// y el paso al catálogo completo sin recargar la página.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, CONSUMIDOR } from './navegador.js';

function oferta(id) {
  return {
    id, establecimientoId: id, establecimiento: `Local ${id}`, distrito: 'Surco', direccion: 'Calle 1',
    calificacion: 4, pack: `Pack ${id}`, descripcion: null, posiblesProductos: 'Variado', alergenos: [], restricciones: [],
    inicioRecojo: '2030-01-10T18:00:00', finRecojo: '2030-01-10T20:00:00', limiteDefinitivo: '2030-01-10T17:00:00',
    estado: 'PUBLICADA', distanciaKm: null,
    variantes: [{ id: id * 10, tamanio: 'MEDIANO', precioReferencial: 10, precioDescuento: 5, ahorro: 5, stockDisponible: 3 }]
  };
}
const CUATRO = [1, 2, 3, 4].map(oferta);

let ofertas = () => CUATRO;
const pagina = abrirPagina('consumidor.html', {
  servidor: {
    'GET /api/sesion': { ...CONSUMIDOR, nombres: '  Valeria Sofía ' },
    'GET /api/notificaciones': [],
    'GET /api/compras/mias': [],
    'GET /api/ofertas': () => ofertas()
  }
});
await import('../app/portal-main.js');
await esperar();

const titulos = () => pagina.$$('#lista .tarjeta-pack h3').map(h => h.textContent);

test('saluda con el primer nombre y muestra solo 3 novedades', () => {
  assert.equal(pagina.$('#usuario').textContent, 'Valeria');
  assert.deepEqual(titulos(), ['Pack 1', 'Pack 2', 'Pack 3']);
  assert.equal(pagina.llamadasA('GET', '/api/ofertas')[0].buscar, '', 'el inicio no tiene filtros');
});

test('«Ver todas» abre el catálogo con todas las ofertas', async () => {
  clic(pagina.$('#ver-todas'));
  await esperar();
  assert.equal(pagina.window.location.pathname, '/app/catalogo.html');
  assert.ok(pagina.$('#filtros'), 'el catálogo trae el formulario de filtros');
  assert.deepEqual(titulos(), ['Pack 1', 'Pack 2', 'Pack 3', 'Pack 4']);
});

test('sin ofertas, el inicio dice que no hay productos por el momento', async () => {
  ofertas = () => [];
  clic(pagina.$('a.portal-marca'));
  await esperar();
  assert.equal(pagina.window.location.pathname, '/app/consumidor.html');
  assert.equal(pagina.$('#lista').textContent, 'No tenemos productos por el momento.');
});

test('una respuesta que llega tarde no pisa la vista nueva', async () => {
  const pendientes = [];
  ofertas = () => new Promise(resolver => pendientes.push(resolver));
  clic(pagina.$('a[href="catalogo.html"]'));
  await esperar();
  clic(pagina.$('a.portal-marca'));
  await esperar();
  assert.equal(pendientes.length, 2, 'cada vista pidió sus ofertas');
  pendientes.forEach(resolver => resolver(CUATRO));
  await esperar();
  assert.deepEqual(titulos(), ['Pack 1', 'Pack 2', 'Pack 3'], 'solo pinta la vista que sigue abierta');
});

test('un error que llega tarde tampoco se muestra', async () => {
  const pendientes = [];
  ofertas = () => new Promise(resolver => pendientes.push(resolver));
  clic(pagina.$('a[href="catalogo.html"]'));
  await esperar();
  clic(pagina.$('a.portal-marca'));
  await esperar();
  pagina.alertas.length = 0;
  pendientes[0](new Error('Failed to fetch'));
  pendientes[1](CUATRO);
  await esperar();
  assert.equal(pagina.alertas.length, 0, 'no avisa de la vista que ya se cerró');
  assert.equal(titulos().length, 3);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
