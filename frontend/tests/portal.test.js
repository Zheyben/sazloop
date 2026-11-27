// Barra del portal del consumidor (portal.js + portal-main.js): foto, carrito, menú de perfil,
// cierre de sesión y cambio de vista sin recargar.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, respuesta, CONSUMIDOR } from './navegador.js';

const USUARIO = { ...CONSUMIDOR, tieneFoto: true };
const CLAVE_CARRITO = `sazloop-carrito-${USUARIO.id}`;
const CARRITO = [
  { clave: 'a', ofertaId: 1, lineas: [{ varianteId: 10, cantidad: 2 }] },
  { clave: 'b', ofertaId: 2, lineas: [{ varianteId: 20, cantidad: 1 }] }
];

const pagina = abrirPagina('notificaciones.html', {
  servidor: {
    'GET /api/sesion': USUARIO,
    'GET /api/notificaciones': [],
    'GET /api/compras/mias': [],
    'GET /api/ofertas': []
  }
});
arreglarUbicacion(pagina);
// El carrito ya estaba guardado de antes de abrir la página.
pagina.window.sessionStorage.setItem(CLAVE_CARRITO, JSON.stringify(CARRITO));
await import('../app/portal-main.js');
await esperar();
const { leerCarrito, guardarCarrito } = await import('../app/portal.js');

const perfil = () => pagina.$('.portal-perfil');
const contador = () => pagina.$('#contador-carrito');

test('con foto de perfil muestra la imagen en vez de las iniciales', () => {
  const foto = pagina.$('.portal-perfil img.avatar-foto');
  assert.ok(foto, 'falta la foto de perfil');
  assert.equal(foto.getAttribute('src'), '/api/perfil/foto');
  assert.equal(pagina.$('.portal-perfil .avatar'), null);
  assert.equal(pagina.$('.nombre-perfil').textContent, 'Valeria');
});

test('el contador del carrito muestra los pedidos guardados en la sesión', () => {
  assert.equal(contador().textContent, '2');
  assert.equal(contador().hidden, false);
});

test('al abrir la página se piden las reseñas pendientes', () => {
  assert.equal(pagina.llamadasA('GET', '/api/compras/mias').length, 1);
});

test('el carrito en el formato antiguo (un solo pedido) se sigue leyendo', () => {
  pagina.window.sessionStorage.setItem(CLAVE_CARRITO,
    JSON.stringify({ ofertaId: 5, lineas: [{ varianteId: 1, cantidad: 3 }, { varianteId: 2, cantidad: 0 }] }));
  assert.deepEqual(leerCarrito(USUARIO.id),
    [{ clave: 'anterior-0', ofertaId: 5, lineas: [{ varianteId: 1, cantidad: 3 }] }]);
});

test('un carrito dañado (JSON inválido) se trata como vacío', () => {
  pagina.window.sessionStorage.setItem(CLAVE_CARRITO, '{esto no es json');
  assert.deepEqual(leerCarrito(USUARIO.id), []);
});

test('guardar el carrito actualiza el contador y vaciarlo lo oculta', () => {
  guardarCarrito(USUARIO.id, CARRITO);
  assert.equal(contador().textContent, '2');
  assert.deepEqual(JSON.parse(pagina.window.sessionStorage.getItem(CLAVE_CARRITO)), CARRITO);
  guardarCarrito(USUARIO.id, []);
  assert.equal(contador().textContent, '');
  assert.equal(contador().hidden, true);
  assert.equal(pagina.window.sessionStorage.getItem(CLAVE_CARRITO), null);
  guardarCarrito(USUARIO.id, CARRITO);
});

test('un clic fuera del menú de perfil lo cierra; un clic dentro no', () => {
  perfil().open = true;
  clic(pagina.$('.menu-perfil'));
  assert.equal(perfil().open, true);
  clic(pagina.$('main.portal-principal'));
  assert.equal(perfil().open, false);
});

test('si cerrar sesión falla (500) avisa «No se pudo cerrar la sesión» y no sale', async () => {
  pagina.servidor['DELETE /api/sesion'] = respuesta(500, { message: 'boom' });
  perfil().open = true;
  clic(pagina.$('#salir'));
  await esperar();
  const alerta = pagina.ultimaAlerta();
  assert.equal(alerta.title, 'No se pudo cerrar la sesión');
  assert.match(alerta.text, /código 500/);
  assert.equal(perfil().open, false);
  assert.deepEqual(pagina.navegaciones, []);
  assert.notEqual(pagina.window.sessionStorage.getItem(CLAVE_CARRITO), null);
});

test('si la sesión ya había vencido (401) igual vacía el carrito y vuelve al acceso', async () => {
  pagina.servidor['DELETE /api/sesion'] = respuesta(401);
  const alertasAntes = pagina.alertas.length;
  clic(pagina.$('#salir'));
  await esperar();
  assert.deepEqual(pagina.navegaciones, ['acceso-consumidor.html']);
  assert.equal(pagina.window.sessionStorage.getItem(CLAVE_CARRITO), null);
  assert.equal(contador().hidden, true);
  assert.equal(pagina.alertas.length, alertasAntes);
});

test('cerrar sesión correctamente vacía el carrito y vuelve al acceso', async () => {
  guardarCarrito(USUARIO.id, CARRITO);
  pagina.navegaciones.length = 0;
  pagina.servidor['DELETE /api/sesion'] = respuesta(204);
  clic(pagina.$('#salir'));
  await esperar();
  assert.equal(pagina.llamadasA('DELETE', '/api/sesion').length, 3);
  assert.deepEqual(pagina.navegaciones, ['acceso-consumidor.html']);
  assert.equal(pagina.window.sessionStorage.getItem(CLAVE_CARRITO), null);
});

test('si la vista no se puede descargar, se abre la página completa', async () => {
  pagina.navegaciones.length = 0;
  pagina.servidor['GET /app/catalogo.html'] = respuesta(500, '');
  clic(pagina.$('.portal-enlaces a[href="catalogo.html"]'));
  await esperar();
  assert.equal(pagina.llamadasA('GET', '/app/catalogo.html').length, 1);
  assert.deepEqual(pagina.navegaciones, ['catalogo.html']);
  assert.equal(pagina.document.title, 'Sazloop | Notificaciones');
});

test('si la vista descargada no tiene contenido principal, se abre la página completa', async () => {
  pagina.navegaciones.length = 0;
  pagina.servidor['GET /app/compras.html'] = '<html><body><p>Sin contenido</p></body></html>';
  clic(pagina.$('.portal-enlaces a[href="compras.html"]'));
  await esperar();
  assert.deepEqual(pagina.navegaciones, ['compras.html']);
});

test('un clic en el enlace de la página actual no vuelve a cargarla', async () => {
  const antes = pagina.llamadas.length;
  clic(pagina.$('a.portal-campana'));
  await esperar();
  assert.equal(pagina.llamadas.length, antes);
  assert.deepEqual(pagina.navegaciones, ['compras.html']);
});

test('al volver atrás en el historial (popstate) se muestra la vista de esa dirección', async () => {
  pagina.window.history.pushState(null, '', '/app/consumidor.html');
  pagina.window.dispatchEvent(new pagina.window.PopStateEvent('popstate'));
  await esperar();
  assert.equal(pagina.llamadasA('GET', '/app/consumidor.html').length, 1);
  assert.equal(pagina.document.title, 'Sazloop | Inicio');
  assert.equal(pagina.$('#usuario').textContent, 'Valeria');
  assert.equal(pagina.$('.portal-enlaces a[href="consumidor.html"]').getAttribute('aria-current'), 'page');
  assert.equal(pagina.$('a.portal-campana').getAttribute('aria-current'), null);
  assert.deepEqual(pagina.navegaciones, ['compras.html']);
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
