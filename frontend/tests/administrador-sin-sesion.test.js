// Panel de administración sin sesión: debe volver a la pantalla de acceso.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, respuesta } from './navegador.js';

const pagina = abrirPagina('administrador.html', {
  servidor: { 'GET /api/administradores/sesion': respuesta(401, { message: 'Sin sesión' }) }
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
  await import('../app/administrador.js');
} catch (error) {
  errorDeCarga = error;
}
await esperar();

test('sin sesión de administración redirige al acceso', () => {
  assert.deepEqual(pagina.navegaciones, ['acceso-administrador.html']);
  assert.equal(errorDeCarga?.message, 'Sin sesión de administración');
});

test('sin sesión no carga las solicitudes ni muestra un nombre', () => {
  assert.equal(pagina.llamadasA('GET', '/api/administradores/solicitudes').length, 0);
  assert.equal(pagina.$('#nombre-admin').textContent, '');
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
