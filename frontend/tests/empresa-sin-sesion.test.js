// Portal de empresa sin sesión iniciada: vuelve al acceso de empresas y locales.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, respuesta } from './navegador.js';

const pagina = abrirPagina('empresa.html', {
  servidor: { 'GET /api/sesion': respuesta(401, { message: 'Unauthorized' }) }
});
let fallo = null;
try {
  await import('../app/empresa.js');
} catch (error) {
  fallo = error;
}
await esperar();

test('sin sesión redirige al acceso de empresas y detiene la carga', () => {
  assert.equal(fallo?.message, 'Sin sesión');
  assert.deepEqual(pagina.navegaciones, ['acceso-locales.html']);
  assert.equal(pagina.llamadasA('GET', '/api/empresas/actual').length, 0, 'no pide la empresa');
});

test('no muestra datos del panel ni el menú lateral', () => {
  assert.equal(pagina.$('#nombre-usuario').textContent, '');
  assert.equal(pagina.$('.panel-lateral').hidden, true);
  assert.equal(pagina.$('#titulo-panel').textContent, 'Tu empresa');
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
