// Compra por QR sin sesión iniciada: manda al acceso del consumidor y vuelve después al mismo QR.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, respuesta } from './navegador.js';

const pagina = abrirPagina('qr.html', {
  buscar: '?oferta=10&token=tok-123',
  servidor: { 'GET /api/sesion': respuesta(401, { message: 'No autenticado' }) }
});
await import('../app/qr.js');
await esperar();

test('sin sesión redirige al acceso con la dirección del QR para continuar', () => {
  assert.deepEqual(pagina.navegaciones,
    ['acceso-consumidor.html?continuar=' + encodeURIComponent('/app/qr.html?oferta=10&token=tok-123')]);
  assert.equal(pagina.llamadas.filter(l => l.ruta.startsWith('/api/ofertas')).length, 0);
  assert.equal(pagina.$('#formQr').hidden, true);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
