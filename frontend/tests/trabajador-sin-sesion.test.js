// Portal del trabajador sin sesión del local: debe volver a la pantalla de acceso.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, respuesta } from './navegador.js';

const pagina = abrirPagina('trabajador.html', {
  servidor: { 'GET /api/local/sesion': respuesta(401, { message: 'Sin sesión' }) }
});
// navegador.js envuelve window.location en un Proxy cuyo «replace» choca con la regla de los Proxy
// (jsdom lo define como propiedad no configurable). Aquí se usa un Proxy sobre un objeto vacío que
// delega en la ubicación real y registra replace/assign igual que navegador.js.
const ubicacionReal = pagina.window.location;
const registrar = destino => pagina.navegaciones.push(new URL(destino, ubicacionReal.href).pathname.split('/').pop());
globalThis.location = new Proxy({}, {
  get(_, propiedad) {
    if (propiedad === 'replace' || propiedad === 'assign') return registrar;
    const valor = Reflect.get(ubicacionReal, propiedad);
    return typeof valor === 'function' ? valor.bind(ubicacionReal) : valor;
  },
  set(_, propiedad, valor) {
    if (propiedad === 'href') { registrar(valor); return true; }
    return Reflect.set(ubicacionReal, propiedad, valor);
  }
});

let errorDeCarga = null;
try {
  await import('../app/trabajador.js');
} catch (error) {
  errorDeCarga = error;
}
await esperar();

test('sin sesión del local redirige al acceso del trabajador', () => {
  assert.deepEqual(pagina.navegaciones, ['acceso-locales.html']);
  assert.equal(errorDeCarga?.message, 'Sin sesión del local');
});

test('sin sesión no pide el resumen del local', () => {
  assert.equal(pagina.llamadasA('GET', '/api/local/sesion').length, 1);
  assert.equal(pagina.llamadasA('GET', '/api/local/resumen').length, 0);
  assert.equal(pagina.$('#titulo-panel').textContent, 'Cargando establecimiento…');
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
