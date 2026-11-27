// Portal del consumidor sin sesión: GET /api/sesion falla y se vuelve a la pantalla de acceso.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, respuesta } from './navegador.js';

const pagina = abrirPagina('consumidor.html', {
  servidor: { 'GET /api/sesion': respuesta(401, { message: 'Unauthorized' }) }
});
arreglarUbicacion(pagina);
await import('../app/portal-main.js');
await esperar();

test('sin sesión manda a la pantalla de acceso del consumidor', () => {
  assert.equal(pagina.llamadasA('GET', '/api/sesion').length, 1);
  assert.deepEqual(pagina.navegaciones, ['acceso-consumidor.html']);
});

test('no arma la barra ni pide datos del portal', () => {
  assert.equal(pagina.$('#portal-nav').children.length, 0);
  assert.equal(pagina.llamadasA('GET', '/api/notificaciones').length, 0);
  assert.equal(pagina.llamadasA('GET', '/api/compras/mias').length, 0);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});

/**
 * El Proxy de location de navegador.js no puede devolver replace/assign propios (en jsdom son
 * propiedades no configurables del objeto Location y el Proxy lanza TypeError). Aquí se reemplaza
 * el global location por un Proxy sobre un objeto vacío que registra esas navegaciones igual.
 */
function arreglarUbicacion(pagina) {
  const real = pagina.window.location;
  const registrar = destino => {
    const url = new URL(destino, real.href);
    pagina.navegaciones.push(url.pathname.split('/').pop() + url.search);
  };
  globalThis.location = new Proxy({}, {
    get(_, propiedad) {
      if (propiedad === 'replace' || propiedad === 'assign') return registrar;
      const valor = real[propiedad];
      return typeof valor === 'function' ? valor.bind(real) : valor;
    },
    set(_, propiedad, valor) {
      if (propiedad === 'href') registrar(valor);
      else real[propiedad] = valor;
      return true;
    }
  });
}
