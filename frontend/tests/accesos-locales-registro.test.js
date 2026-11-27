// Acceso de locales abierto con #registro: pestaña empresa y formulario de registro.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar } from './navegador.js';

const pagina = abrirPagina('acceso-locales.html', { hash: '#registro' });
await import('../app/accesos.js');
await esperar();

test('con #registro abre la pestaña empresa en «Crear cuenta»', () => {
  assert.equal(pagina.$('[data-rol="empresa"]').getAttribute('aria-pressed'), 'true');
  assert.ok(pagina.$('#marco-local').classList.contains('modo-empresa'));
  assert.equal(pagina.$('#ingreso-trabajador').hidden, true);
  assert.equal(pagina.$('#registro-personal').hidden, false);
  assert.equal(pagina.$('#ingreso-personal').hidden, true);
  assert.equal(pagina.$('#titulo-formulario').textContent, 'Crear cuenta');
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
