// Compra por QR con un enlace incompleto (sin token).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, CONSUMIDOR } from './navegador.js';

const pagina = abrirPagina('qr.html', {
  buscar: '?oferta=abc',
  servidor: { 'GET /api/sesion': CONSUMIDOR }
});
await import('../app/qr.js');
await esperar();

test('un enlace QR sin oferta válida ni token muestra que no es válido', () => {
  assert.equal(pagina.$('#ofertaQr').textContent, 'El enlace QR no es válido.');
  const alerta = pagina.alertas.find(a => a.title === 'Enlace no válido');
  assert.equal(alerta.icon, 'error');
  assert.equal(alerta.text, 'Este código QR no corresponde a una oferta de Sazloop.');
  assert.equal(pagina.llamadas.filter(l => l.ruta.startsWith('/api/ofertas')).length, 0);
  assert.equal(pagina.$('#formQr').hidden, true);
  assert.deepEqual(pagina.navegaciones, []);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
