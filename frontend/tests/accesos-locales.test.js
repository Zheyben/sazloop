// Acceso de locales (app/acceso-locales.html + accesos.js): pestañas trabajador / empresa.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, llenarYEnviar, clic, respuesta } from './navegador.js';

const pagina = abrirPagina('acceso-locales.html', {
  servidor: {
    'POST /api/local/sesion': respuesta(204),
    'POST /api/sesion': respuesta(204),
    'POST /api/usuarios': respuesta(201, { id: 3 })
  }
});
await import('../app/accesos.js');
await esperar();

const boton = rol => pagina.$(`[data-rol="${rol}"]`);
const marco = () => pagina.$('#marco-local');

test('sin hash empieza en la pestaña del trabajador', () => {
  assert.equal(pagina.$('#ingreso-trabajador').hidden, false);
  assert.equal(pagina.$('#ingreso-personal').hidden, true);
  assert.equal(pagina.$('#ir-registro').hidden, true);
  assert.equal(pagina.$('#ayuda-trabajador').hidden, false);
  assert.equal(pagina.$('#titulo-formulario').textContent, 'Ingresar como trabajador');
  assert.equal(pagina.$('#etiqueta-historia').textContent, 'PARA EL TRABAJADOR');
  assert.equal(boton('trabajador').getAttribute('aria-pressed'), 'true');
  assert.ok(marco().classList.contains('modo-trabajador'));
});

test('el trabajador ingresa con su usuario y abre su pantalla', async () => {
  llenarYEnviar(pagina.$('#ingreso-trabajador'), { nombreUsuario: 'local.miraflores', contrasena: 'clave-local' });
  await esperar();
  const llamadas = pagina.llamadasA('POST', '/api/local/sesion');
  assert.equal(llamadas.length, 1);
  assert.deepEqual(llamadas[0].cuerpo, { nombreUsuario: 'local.miraflores', contrasena: 'clave-local' });
  assert.deepEqual(pagina.navegaciones, ['trabajador.html']);
});

test('usuario de local incorrecto (401) avisa y se queda en la página', async () => {
  pagina.servidor['POST /api/local/sesion'] = respuesta(401);
  pagina.navegaciones.length = 0;
  llenarYEnviar(pagina.$('#ingreso-trabajador'), { nombreUsuario: 'local.x', contrasena: 'mala' });
  await esperar();
  assert.deepEqual(pagina.navegaciones, []);
  assert.equal(pagina.ultimaAlerta().title, 'No pudimos iniciar sesión');
  assert.equal(pagina.ultimaAlerta().text, 'El correo o la contraseña no coinciden. Revísalos e intenta de nuevo.');
  assert.equal(pagina.$('#mensaje-acceso').className, 'error');
});

test('la pestaña «Empresa» cambia los textos y muestra el ingreso con correo', () => {
  clic(boton('empresa'));
  assert.equal(pagina.$('#ingreso-trabajador').hidden, true);
  assert.equal(pagina.$('#ingreso-personal').hidden, false);
  assert.equal(pagina.$('#ir-registro').hidden, false);
  assert.equal(pagina.$('#ayuda-trabajador').hidden, true);
  assert.equal(pagina.$('#titulo-formulario').textContent, 'Ingresar como empresa');
  assert.equal(pagina.$('#etiqueta-historia').textContent, 'PARA LAS EMPRESAS');
  assert.equal(pagina.$('#titulo-historia').textContent, 'Tu excedente todavía tiene valor.');
  assert.match(pagina.$('#descripcion-historia').textContent, /Gestiona tu empresa/);
  assert.equal(boton('empresa').getAttribute('aria-pressed'), 'true');
  assert.equal(boton('trabajador').getAttribute('aria-pressed'), 'false');
  assert.ok(marco().classList.contains('modo-empresa'));
  assert.equal(pagina.$('#mensaje-acceso').textContent, '');
});

test('la empresa ingresa con su correo y abre la pantalla de empresa', async () => {
  pagina.navegaciones.length = 0;
  llenarYEnviar(pagina.$('#ingreso-personal'), { email: 'empresa@sazloop.test', contrasena: 'clave-empresa' });
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/sesion').length, 1);
  assert.deepEqual(pagina.navegaciones, ['empresa.html']);
});

test('la empresa puede crear su cuenta y vuelve a «Ingresar como empresa»', async () => {
  clic(pagina.$('#ir-registro a'));
  assert.equal(pagina.$('#registro-personal').hidden, false);
  assert.equal(pagina.$('#titulo-formulario').textContent, 'Crear cuenta');
  llenarYEnviar(pagina.$('#registro-personal'),
    { nombres: 'Luis', apellidos: 'Paz', email: 'luis@sazloop.test', telefono: '', contrasena: 'clave-luis-1' });
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/usuarios').length, 1);
  assert.equal(pagina.$('#registro-personal').hidden, true);
  assert.equal(pagina.$('#ingreso-personal').hidden, false);
  assert.equal(pagina.$('#ingreso-personal').elements.email.value, 'luis@sazloop.test');
  assert.equal(pagina.$('#titulo-formulario').textContent, 'Ingresar como empresa');
  assert.equal(pagina.ultimaAlerta().title, 'Cuenta creada');
});

test('volver a la pestaña «Trabajador» oculta los formularios de la empresa', () => {
  clic(boton('trabajador'));
  assert.equal(pagina.$('#ingreso-trabajador').hidden, false);
  assert.equal(pagina.$('#ingreso-personal').hidden, true);
  assert.equal(pagina.$('#registro-personal').hidden, true);
  assert.equal(pagina.$('#titulo-historia').textContent, 'Tu local, en marcha.');
  assert.equal(pagina.$('#titulo-formulario').textContent, 'Ingresar como trabajador');
  assert.ok(marco().classList.contains('modo-trabajador'));
  assert.ok(!marco().classList.contains('modo-empresa'));
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
