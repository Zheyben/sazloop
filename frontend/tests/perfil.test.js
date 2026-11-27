// Pantalla «Mi perfil» del consumidor (app/perfil.html + perfil.js): datos, foto y contraseña.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, respuesta, CONSUMIDOR } from './navegador.js';

const pagina = abrirPagina('perfil.html', {
  servidor: {
    'GET /api/sesion': { ...CONSUMIDOR, telefono: null },
    'GET /api/notificaciones': [],
    'GET /api/compras/mias': [],
    'GET /api/ofertas': [],
    'PUT /api/perfil': respuesta(400, { message: 'El correo ya está en uso' }),
    'PUT /api/perfil/contrasena': new Error('Failed to fetch'),
    'POST /api/perfil/foto': respuesta(413)
  }
});
await import('../app/portal-main.js');
await esperar();

const $ = pagina.$;
const enviar = form => form.dispatchEvent(new pagina.window.Event('submit', { bubbles: true, cancelable: true }));
function elegirArchivo(form, archivo) {
  Object.defineProperty(form.elements.archivo, 'files', { configurable: true, value: archivo ? [archivo] : [] });
}

test('muestra los datos de la cuenta', () => {
  assert.equal($('#perfil-nombre-completo').textContent, 'Valeria Quispe');
  assert.equal($('#perfil-correo-corto').textContent, CONSUMIDOR.email);
  assert.equal($('#perfil-iniciales').textContent, 'VQ');
  assert.equal($('#perfil-telefono').textContent, 'No registrado');
  assert.equal($('#perfil-foto').hidden, true, 'sin foto se ven las iniciales');
  assert.equal($('#form-perfil').elements.nombres.value, 'Valeria');
  assert.equal($('#form-perfil').elements.telefono.value, '');
});

test('«Modificar información» abre el formulario y «Cancelar» lo cierra sin cambios', () => {
  clic($('#editar-perfil'));
  assert.equal($('#form-perfil').hidden, false);
  assert.equal($('#datos-perfil').hidden, true);
  $('#form-perfil').elements.nombres.value = 'Otro';
  clic($('#cancelar-perfil'));
  assert.equal($('#form-perfil').hidden, true);
  assert.equal($('#datos-perfil').hidden, false);
  assert.equal($('#form-perfil').elements.nombres.value, 'Valeria');
});

test('si el servidor rechaza los datos, muestra el motivo', async () => {
  clic($('#editar-perfil'));
  pagina.alertas.length = 0;
  $('#form-perfil').elements.email.value = 'otro@sazloop.test';
  enviar($('#form-perfil'));
  await esperar();
  const llamada = pagina.llamadasA('PUT', '/api/perfil').at(-1);
  assert.equal(llamada.cuerpo.email, 'otro@sazloop.test');
  assert.equal($('#mensaje-perfil').textContent, 'El correo ya está en uso');
  assert.equal($('#mensaje-perfil').className, 'error');
  assert.ok(pagina.alertas.some(a => a.icon === 'error' && a.title === 'No se pudieron guardar tus datos'));
  assert.equal($('#form-perfil').hidden, false);
  assert.equal($('#form-perfil').querySelector('button[type="submit"]').disabled, false);
});

test('guardar los datos actualiza el perfil y la barra del portal', async () => {
  pagina.servidor['PUT /api/perfil'] = cuerpo => ({ id: CONSUMIDOR.id, tieneFoto: false, ...cuerpo });
  pagina.alertas.length = 0;
  const form = $('#form-perfil');
  form.elements.nombres.value = 'Ana Lucía';
  form.elements.apellidos.value = 'Torres';
  form.elements.email.value = 'ana@sazloop.test';
  form.elements.telefono.value = '987654321';
  enviar(form);
  await esperar();
  assert.deepEqual(pagina.llamadasA('PUT', '/api/perfil').at(-1).cuerpo,
    { nombres: 'Ana Lucía', apellidos: 'Torres', email: 'ana@sazloop.test', telefono: '987654321' });
  assert.equal($('#perfil-nombre-completo').textContent, 'Ana Lucía Torres');
  assert.equal($('#perfil-telefono').textContent, '987654321');
  assert.equal($('.nombre-perfil').textContent, 'Ana');
  assert.equal($('.avatar').textContent, 'AT');
  assert.equal(form.hidden, true);
  assert.equal($('#mensaje-perfil').className, 'ok');
  assert.ok(pagina.alertas.some(a => a.icon === 'success' && a.title === 'Datos actualizados'
    && a.text === 'Tus datos ya quedaron guardados.'));
});

test('cambiar la contraseña sin conexión avisa que no hay internet', async () => {
  clic($('#editar-contrasena'));
  assert.equal($('#form-contrasena').hidden, false);
  pagina.alertas.length = 0;
  const form = $('#form-contrasena');
  form.elements.actual.value = 'Clave123!';
  form.elements.nueva.value = 'NuevaClave456!';
  enviar(form);
  await esperar();
  assert.equal($('#mensaje-perfil').textContent, 'Sin conexión. Revisa tu internet e intenta de nuevo.');
  assert.ok(pagina.alertas.some(a => a.title === 'Sin conexión'));
  assert.equal(form.hidden, false);
});

test('cambiar la contraseña bien limpia el formulario y lo oculta', async () => {
  pagina.servidor['PUT /api/perfil/contrasena'] = respuesta(204);
  pagina.alertas.length = 0;
  const form = $('#form-contrasena');
  enviar(form);
  await esperar();
  assert.deepEqual(pagina.llamadasA('PUT', '/api/perfil/contrasena').at(-1).cuerpo,
    { actual: 'Clave123!', nueva: 'NuevaClave456!' });
  assert.equal(form.hidden, true);
  assert.equal(form.elements.actual.value, '');
  assert.ok(pagina.alertas.some(a => a.icon === 'success' && a.title === 'Contraseña actualizada'));
});

test('una foto que no es PNG o JPG pide revisarla sin llamar al servidor', () => {
  clic($('#editar-foto'));
  assert.equal($('#form-foto').hidden, false);
  pagina.alertas.length = 0;
  elegirArchivo($('#form-foto'), new pagina.window.File(['x'], 'foto.gif', { type: 'image/gif' }));
  enviar($('#form-foto'));
  assert.ok(pagina.alertas.some(a => a.icon === 'warning' && a.title === 'Revisa la foto'
    && a.text === 'Elige una foto PNG o JPG de hasta 2 MB.'));
  assert.equal(pagina.llamadasA('POST', '/api/perfil/foto').length, 0);
});

test('sin elegir foto también pide revisarla', () => {
  pagina.alertas.length = 0;
  elegirArchivo($('#form-foto'), null);
  enviar($('#form-foto'));
  assert.ok(pagina.alertas.some(a => a.title === 'Revisa la foto'));
});

test('si el servidor rechaza la foto, muestra el error', async () => {
  pagina.alertas.length = 0;
  elegirArchivo($('#form-foto'), new pagina.window.File(['png'], 'foto.png', { type: 'image/png' }));
  enviar($('#form-foto'));
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/perfil/foto').length, 1);
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudo actualizar la foto'
    && a.text === 'El archivo supera el tamaño permitido.'));
  assert.equal($('#perfil-foto').hidden, true);
});

test('subir una foto válida la muestra en el perfil y en la barra', async () => {
  pagina.servidor['POST /api/perfil/foto'] = respuesta(204);
  pagina.alertas.length = 0;
  enviar($('#form-foto'));
  await esperar();
  const llamada = pagina.llamadasA('POST', '/api/perfil/foto').at(-1);
  assert.ok(llamada.cuerpo instanceof pagina.window.FormData, 'la foto se manda como formulario');
  assert.equal($('#perfil-foto').hidden, false);
  assert.match($('#perfil-foto').src, /\/api\/perfil\/foto\?t=\d+$/);
  assert.equal($('#perfil-iniciales').hidden, true);
  assert.ok($('.portal-perfil summary img.avatar-foto'), 'la barra usa la foto');
  assert.equal($('#form-foto').hidden, true);
  assert.ok(pagina.alertas.some(a => a.icon === 'success' && a.title === 'Foto actualizada'));
});

test('al volver al perfil con foto, se muestra la foto', async () => {
  clic($('a.portal-marca'));
  await esperar();
  clic($('.menu-perfil a[href="perfil.html"]'));
  await esperar();
  assert.equal(pagina.window.location.pathname, '/app/perfil.html');
  assert.equal($('#perfil-foto').hidden, false);
  assert.equal($('#perfil-nombre-completo').textContent, 'Ana Lucía Torres');
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
