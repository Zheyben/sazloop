// Compra por QR cuando la oferta ya no se puede abrir.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, respuesta, CONSUMIDOR } from './navegador.js';

const pagina = abrirPagina('qr.html', {
  buscar: '?oferta=55&token=tok-9',
  servidor: {
    'GET /api/sesion': CONSUMIDOR,
    'GET /api/ofertas/55': respuesta(404, { message: 'La oferta ya no está publicada' })
  }
});
await import('../app/qr.js');
await esperar();

test('si la oferta no se puede abrir, muestra el motivo y no enseña el formulario', () => {
  assert.equal(pagina.llamadasA('GET', '/api/ofertas/55').length, 1);
  assert.equal(pagina.$('#ofertaQr').textContent, 'La oferta ya no está publicada');
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudo abrir la oferta' && a.text === 'La oferta ya no está publicada'));
  assert.equal(pagina.$('#formQr').hidden, true);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
