// Reseñas pendientes (app/resenas-portal.js): al entrar al portal se pide calificar cada local
// con una compra recogida sin reseña (una vez por local).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, respuesta, CONSUMIDOR } from './navegador.js';

const COMPRAS = [
  { reservaId: 50, establecimientoId: 1, establecimiento: 'Panadería Sol', resenaPendiente: true },
  { reservaId: 51, establecimientoId: 1, establecimiento: 'Panadería Sol', resenaPendiente: true },
  { reservaId: 52, establecimientoId: 2, establecimiento: 'Café Luna', resenaPendiente: true },
  { reservaId: 53, establecimientoId: 3, establecimiento: 'Pizzería Mar', resenaPendiente: false }
];

const pagina = abrirPagina('notificaciones.html', {
  servidor: {
    'GET /api/sesion': CONSUMIDOR,
    'GET /api/notificaciones': [],
    'GET /api/compras/mias': COMPRAS,
    'POST /api/compras/50/resena': respuesta(500, { message: 'Error' }),
    'POST /api/compras/52/resena/omitir': new Error('Failed to fetch')
  }
});
await import('../app/portal-main.js');
await esperar();

const dialogo = () => pagina.$('#dialogo-resena');
const formulario = () => dialogo().querySelector('form');
const enviar = () => formulario().dispatchEvent(new pagina.window.Event('submit', { bubbles: true, cancelable: true }));
const estrella = n => pagina.$(`.resena-estrella[data-valor="${n}"]`);

test('abre la reseña del primer local pendiente', () => {
  assert.equal(dialogo().open, true);
  assert.equal(pagina.$('#titulo-resena').textContent, '¿Cómo calificarías el producto de Panadería Sol?');
  assert.equal(pagina.$('#leyenda-resena').textContent, 'Toca una estrella');
  assert.equal(pagina.$$('.resena-estrella').length, 5);
});

test('sin estrellas avisa que falta la calificación y no llama al servidor', () => {
  pagina.alertas.length = 0;
  enviar();
  assert.equal(pagina.$('#mensaje-resena').textContent, 'Elige de 1 a 5 estrellas para enviar tu reseña.');
  assert.ok(pagina.alertas.some(a => a.title === 'Falta la calificación'));
  assert.equal(pagina.llamadasA('POST', '/api/compras/50/resena').length, 0);
});

test('tocar una estrella marca las anteriores y muestra la leyenda', () => {
  clic(estrella(4));
  assert.equal(formulario().elements.calificacion.value, '4');
  assert.equal(pagina.$('#leyenda-resena').textContent, '4 de 5 · Bueno');
  assert.equal(pagina.$$('.resena-estrella.activa').length, 4);
  assert.equal(estrella(4).getAttribute('aria-checked'), 'true');
  assert.equal(estrella(3).getAttribute('aria-checked'), 'false');
});

test('si el servidor falla al enviar, muestra el error y deja el diálogo abierto', async () => {
  pagina.alertas.length = 0;
  formulario().elements.comentario.value = '  Muy rico todo  ';
  enviar();
  await esperar();
  const llamada = pagina.llamadasA('POST', '/api/compras/50/resena').at(-1);
  assert.deepEqual(llamada.cuerpo, { calificacion: 4, comentario: 'Muy rico todo' });
  assert.equal(dialogo().open, true);
  assert.match(pagina.$('#mensaje-resena').textContent, /error inesperado en el servidor/);
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudo enviar la reseña'));
  assert.equal(formulario().querySelector('button[type="submit"]').disabled, false);
});

test('al enviar bien, agradece y pasa al siguiente local (una vez por local)', async () => {
  pagina.servidor['POST /api/compras/50/resena'] = respuesta(204);
  pagina.alertas.length = 0;
  enviar();
  await esperar();
  assert.ok(pagina.alertas.some(a => a.icon === 'success' && a.title === 'Gracias por tu reseña'));
  assert.equal(dialogo().open, true);
  assert.equal(pagina.$('#titulo-resena').textContent, '¿Cómo calificarías el producto de Café Luna?');
  assert.equal(formulario().elements.calificacion.value, '', 'el formulario vuelve a empezar');
  assert.equal(pagina.$('#leyenda-resena').textContent, 'Toca una estrella');
  assert.equal(pagina.llamadasA('POST', '/api/compras/51/resena').length, 0);
});

test('omitir sin conexión avisa y no cierra el diálogo', async () => {
  pagina.alertas.length = 0;
  clic(pagina.$('.resena-cerrar'));
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/compras/52/resena/omitir').length, 1);
  assert.equal(pagina.$('#mensaje-resena').textContent, 'Sin conexión. Revisa tu internet e intenta de nuevo.');
  assert.ok(pagina.alertas.some(a => a.title === 'Sin conexión'));
  assert.equal(dialogo().open, true);
});

test('cerrar con Escape omite la reseña y cierra el diálogo', async () => {
  pagina.servidor['POST /api/compras/52/resena/omitir'] = respuesta(204);
  pagina.alertas.length = 0;
  const evento = new pagina.window.Event('cancel', { cancelable: true });
  dialogo().dispatchEvent(evento);
  await esperar();
  assert.equal(evento.defaultPrevented, true);
  const llamada = pagina.llamadasA('POST', '/api/compras/52/resena/omitir').at(-1);
  assert.equal(llamada.cuerpo, undefined, 'omitir no manda datos');
  assert.equal(dialogo().open, false);
  assert.equal(pagina.alertas.length, 0, 'omitir no muestra el agradecimiento');
  assert.equal(pagina.llamadasA('POST', '/api/compras/53/resena').length, 0);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
