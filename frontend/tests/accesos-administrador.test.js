// Acceso del administrador (app/acceso-administrador.html + accesos.js).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, llenarYEnviar, respuesta } from './navegador.js';

const pagina = abrirPagina('acceso-administrador.html', {
  servidor: { 'POST /api/administradores/sesion': respuesta(401) }
});
await import('../app/accesos.js');
await esperar();

const formulario = () => pagina.$('#ingreso-administrador');
const DATOS = { email: 'admin@sazloop.test', contrasena: 'clave-admin' };

test('credenciales incorrectas (401) avisan y no cambian de página', async () => {
  llenarYEnviar(formulario(), DATOS);
  await esperar();
  assert.deepEqual(pagina.navegaciones, []);
  assert.equal(pagina.$('#mensaje-acceso').textContent, 'Revisa tus credenciales e intenta de nuevo.');
  assert.equal(pagina.ultimaAlerta().title, 'No pudimos iniciar sesión');
});

test('demasiados intentos (429) muestra el bloqueo temporal', async () => {
  pagina.servidor['POST /api/administradores/sesion'] = respuesta(429, { message: 'Bloqueado 5 minutos.' });
  llenarYEnviar(formulario(), DATOS);
  await esperar();
  assert.equal(pagina.$('#mensaje-acceso').textContent, 'Bloqueado 5 minutos.');
  assert.equal(pagina.ultimaAlerta().text, 'Bloqueado 5 minutos.');
});

test('sin internet avisa «Sin conexión»', async () => {
  pagina.servidor['POST /api/administradores/sesion'] = new TypeError('Failed to fetch');
  llenarYEnviar(formulario(), DATOS);
  await esperar();
  assert.equal(pagina.ultimaAlerta().title, 'Sin conexión');
  assert.equal(pagina.$('#mensaje-acceso').textContent, 'Sin conexión. Revisa tu internet e intenta de nuevo.');
});

test('con credenciales correctas abre el panel del administrador', async () => {
  pagina.servidor['POST /api/administradores/sesion'] = respuesta(204);
  llenarYEnviar(formulario(), DATOS);
  await esperar();
  const llamadas = pagina.llamadasA('POST', '/api/administradores/sesion');
  assert.equal(llamadas.length, 4);
  assert.deepEqual(llamadas.at(-1).cuerpo, DATOS);
  assert.deepEqual(pagina.navegaciones, ['administrador.html']);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
