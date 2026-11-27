// Acceso del consumidor abierto con #registro: empieza directamente en «Crear cuenta».
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar } from './navegador.js';

const pagina = abrirPagina('acceso-consumidor.html', { hash: '#registro' });
await import('../app/accesos.js');
await esperar();

test('con #registro se abre el formulario para crear la cuenta', () => {
  assert.equal(pagina.$('#registro-personal').hidden, false);
  assert.equal(pagina.$('#ingreso-personal').hidden, true);
  assert.equal(pagina.$('#ir-ingreso').hidden, false);
  assert.equal(pagina.$('#titulo-formulario').textContent, 'Crear cuenta');
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
