// Pantalla de acceso del consumidor (app/acceso-consumidor.html + accesos.js): ingreso, registro y errores.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, llenarYEnviar, clic, respuesta } from './navegador.js';

const pagina = abrirPagina('acceso-consumidor.html', {
  servidor: {
    'POST /api/sesion': respuesta(204),
    'POST /api/usuarios': respuesta(201, { id: 9 })
  }
});
await import('../app/accesos.js');
await esperar();

const ingreso = () => pagina.$('#ingreso-personal');
const registro = () => pagina.$('#registro-personal');
const mensaje = () => pagina.$('#mensaje-acceso');
const CREDENCIALES = { email: 'valeria@sazloop.test', contrasena: 'clave-segura-1' };
const DATOS_REGISTRO = { nombres: 'Ana', apellidos: 'Rojas', email: 'ana@sazloop.test', telefono: '999888777', contrasena: 'clave-segura-2' };

/** Cambia el ?continuar= de la dirección actual sin recargar la página. */
function ponerContinuar(valor) {
  const busqueda = valor === null ? '' : `?continuar=${encodeURIComponent(valor)}`;
  pagina.window.history.replaceState(null, '', `/app/acceso-consumidor.html${busqueda}`);
}

async function ingresar() {
  pagina.navegaciones.length = 0;
  pagina.alertas.length = 0;
  llenarYEnviar(ingreso(), CREDENCIALES);
  await esperar();
}

test('al abrir se ve el formulario de ingreso y el de registro está oculto', () => {
  assert.equal(ingreso().hidden, false);
  assert.equal(registro().hidden, true);
  assert.equal(pagina.$('#titulo-formulario').textContent, 'Iniciar sesión');
});

test('ingresar con datos correctos envía las credenciales y abre el portal del consumidor', async () => {
  await ingresar();
  const llamadas = pagina.llamadasA('POST', '/api/sesion');
  assert.equal(llamadas.length, 1);
  assert.deepEqual(llamadas[0].cuerpo, CREDENCIALES);
  assert.equal(llamadas[0].opciones.credentials, 'include');
  assert.deepEqual(pagina.navegaciones, ['consumidor.html']);
  assert.equal(ingreso().querySelector('button[type="submit"]').disabled, false);
});

test('con ?continuar= hacia el QR, después de ingresar vuelve a esa página', async () => {
  ponerContinuar('/app/qr.html?codigo=AB12');
  await ingresar();
  assert.deepEqual(pagina.navegaciones, ['qr.html?codigo=AB12']);
});

test('un ?continuar= hacia otro sitio se ignora y abre el portal normal', async () => {
  ponerContinuar('https://sitio-malicioso.example/app/qr.html?codigo=AB12');
  await ingresar();
  assert.deepEqual(pagina.navegaciones, ['consumidor.html']);
});

test('un ?continuar= hacia otra página propia que no es el QR también se ignora', async () => {
  ponerContinuar('/app/administrador.html');
  await ingresar();
  assert.deepEqual(pagina.navegaciones, ['consumidor.html']);
  ponerContinuar(null);
});

test('credenciales incorrectas (401) muestran el aviso y no cambian de página', async () => {
  pagina.servidor['POST /api/sesion'] = respuesta(401, { message: 'Unauthorized' });
  await ingresar();
  assert.deepEqual(pagina.navegaciones, []);
  assert.equal(mensaje().textContent, 'Revisa tus credenciales e intenta de nuevo.');
  assert.equal(mensaje().className, 'error');
  const alerta = pagina.ultimaAlerta();
  assert.equal(alerta.title, 'No pudimos iniciar sesión');
  assert.equal(alerta.text, 'El correo o la contraseña no coinciden. Revísalos e intenta de nuevo.');
  assert.equal(alerta.icon, 'error');
});

test('cuenta bloqueada (429) muestra el mensaje que manda el servidor', async () => {
  pagina.servidor['POST /api/sesion'] = respuesta(429, { message: 'Cuenta bloqueada por 5 minutos.' });
  await ingresar();
  assert.equal(mensaje().textContent, 'Cuenta bloqueada por 5 minutos.');
  assert.equal(pagina.ultimaAlerta().title, 'No pudimos iniciar sesión');
  assert.equal(pagina.ultimaAlerta().text, 'Cuenta bloqueada por 5 minutos.');
});

test('cuenta bloqueada (429) sin mensaje del servidor muestra el texto por defecto', async () => {
  pagina.servidor['POST /api/sesion'] = respuesta(429);
  await ingresar();
  assert.equal(mensaje().textContent, 'Demasiados intentos fallidos. Espera 5 minutos e inténtalo de nuevo.');
  assert.equal(pagina.ultimaAlerta().text, 'Demasiados intentos fallidos. Espera 5 minutos e inténtalo de nuevo.');
});

test('un error del servidor (500) al ingresar muestra un mensaje genérico', async () => {
  pagina.servidor['POST /api/sesion'] = respuesta(500, { message: 'boom' });
  await ingresar();
  assert.equal(mensaje().textContent, 'No se pudo completar la operación.');
  assert.equal(pagina.ultimaAlerta().text, 'No se pudo completar la operación.');
});

test('sin internet avisa «Sin conexión»', async () => {
  pagina.servidor['POST /api/sesion'] = new TypeError('Failed to fetch');
  await ingresar();
  assert.deepEqual(pagina.navegaciones, []);
  assert.equal(mensaje().textContent, 'Sin conexión. Revisa tu internet e intenta de nuevo.');
  assert.equal(pagina.ultimaAlerta().title, 'Sin conexión');
  assert.equal(pagina.ultimaAlerta().icon, 'error');
  assert.equal(ingreso().querySelector('button[type="submit"]').disabled, false);
});

test('el enlace «Regístrate aquí» muestra el formulario de registro', () => {
  clic(pagina.$('#ir-registro a'));
  assert.equal(ingreso().hidden, true);
  assert.equal(registro().hidden, false);
  assert.equal(pagina.$('#ir-registro').hidden, true);
  assert.equal(pagina.$('#ir-ingreso').hidden, false);
  assert.equal(pagina.$('#titulo-formulario').textContent, 'Crear cuenta');
  assert.equal(mensaje().textContent, '');
});

test('registro con correo repetido (409) avisa que la cuenta ya existe', async () => {
  pagina.servidor['POST /api/usuarios'] = respuesta(409, { message: 'Conflict' });
  pagina.alertas.length = 0;
  llenarYEnviar(registro(), DATOS_REGISTRO);
  await esperar();
  assert.equal(pagina.ultimaAlerta().title, 'Esta cuenta ya existe');
  assert.equal(pagina.ultimaAlerta().icon, 'warning');
  assert.equal(mensaje().textContent, 'La cuenta ya existe o no se puede completar la operación.');
  assert.equal(registro().hidden, false);
});

test('registro con datos inválidos (400) pide revisar los datos', async () => {
  pagina.servidor['POST /api/usuarios'] = respuesta(400, { message: 'Validation failed' });
  llenarYEnviar(registro(), DATOS_REGISTRO);
  await esperar();
  const alerta = pagina.ultimaAlerta();
  assert.equal(alerta.title, 'No se pudo crear la cuenta');
  assert.equal(alerta.icon, 'warning');
  assert.equal(alerta.text, 'Revisa los datos ingresados.');
});

test('registro con error del servidor (500) muestra un error', async () => {
  pagina.servidor['POST /api/usuarios'] = respuesta(500);
  llenarYEnviar(registro(), DATOS_REGISTRO);
  await esperar();
  const alerta = pagina.ultimaAlerta();
  assert.equal(alerta.title, 'No se pudo crear la cuenta');
  assert.equal(alerta.icon, 'error');
  assert.equal(alerta.text, 'No se pudo completar la operación.');
});

test('registro correcto crea la cuenta, vuelve al ingreso y deja el correo escrito', async () => {
  pagina.servidor['POST /api/usuarios'] = respuesta(201, { id: 9 });
  const antes = pagina.llamadasA('POST', '/api/usuarios').length;
  llenarYEnviar(registro(), DATOS_REGISTRO);
  await esperar();
  const llamadas = pagina.llamadasA('POST', '/api/usuarios');
  assert.equal(llamadas.length, antes + 1);
  assert.deepEqual(llamadas.at(-1).cuerpo, DATOS_REGISTRO);
  assert.equal(registro().hidden, true);
  assert.equal(ingreso().hidden, false);
  assert.equal(ingreso().elements.email.value, 'ana@sazloop.test');
  assert.equal(registro().elements.nombres.value, '');
  assert.equal(mensaje().textContent, 'Cuenta creada. Ya puedes iniciar sesión.');
  assert.equal(mensaje().className, 'ok');
  assert.equal(pagina.ultimaAlerta().title, 'Cuenta creada');
  assert.equal(pagina.ultimaAlerta().icon, 'success');
});

test('el enlace «Inicia sesión» vuelve del registro al ingreso', () => {
  clic(pagina.$('#ir-registro a'));
  assert.equal(registro().hidden, false);
  clic(pagina.$('#ir-ingreso a'));
  assert.equal(registro().hidden, true);
  assert.equal(ingreso().hidden, false);
  assert.equal(pagina.$('#titulo-formulario').textContent, 'Iniciar sesión');
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
