// Pantalla «Mis notificaciones» del consumidor (app/notificaciones.html + notificaciones.js).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, respuesta, CONSUMIDOR } from './navegador.js';

const AVISOS = [
  { id: 1, titulo: 'Recoge tu pedido', mensaje: 'Tu código es ABC', fecha: '2026-11-26T18:00:00', leida: false },
  { id: 2, titulo: 'Compra confirmada', mensaje: 'Gracias por tu compra', fecha: '2026-11-25T12:00:00', leida: true }
];

let avisos = structuredClone(AVISOS);
const pagina = abrirPagina('notificaciones.html', {
  servidor: {
    'GET /api/sesion': CONSUMIDOR,
    'GET /api/notificaciones': () => avisos,
    'GET /api/compras/mias': [],
    'POST /api/notificaciones/1/leer': () => { avisos[0].leida = true; return respuesta(204); },
    'POST /api/notificaciones/leer-todas': () => { avisos.forEach(a => { a.leida = true; }); return respuesta(204); }
  }
});
await import('../app/portal-main.js');
await esperar();

test('muestra las notificaciones y cuántas faltan leer', () => {
  assert.match(pagina.texto(), /Recoge tu pedido/);
  assert.match(pagina.texto(), /Compra confirmada/);
  assert.equal(pagina.$$('.aviso-item.no-leida').length, 1);
  assert.equal(pagina.$('#contador-avisos').textContent, '1');
});

test('marcar una como leída llama al servidor y vuelve a cargar', async () => {
  const boton = pagina.$$('.aviso-item button').find(b => b.textContent === 'Marcar como leída');
  clic(boton);
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/notificaciones/1/leer').length, 1);
  assert.equal(pagina.$$('.aviso-item.no-leida').length, 0);
  assert.equal(pagina.$('#contador-avisos').hidden, true);
});

test('marcar todas como leídas', async () => {
  avisos = structuredClone(AVISOS);
  pagina.servidor['GET /api/notificaciones'] = () => avisos;
  clic(pagina.$('a[href="consumidor.html"]'));
  await esperar();
  clic(pagina.$('a.portal-campana'));
  await esperar();
  const boton = pagina.$$('button').find(b => b.textContent === 'Marcar todas como leídas');
  assert.ok(boton, 'falta el botón «Marcar todas como leídas»');
  clic(boton);
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/notificaciones/leer-todas').length, 1);
  assert.equal(pagina.$$('.aviso-item.no-leida').length, 0);
});

test('sin notificaciones muestra el estado vacío', async () => {
  pagina.servidor['GET /api/notificaciones'] = [];
  clic(pagina.$('a[href="consumidor.html"]'));
  await esperar();
  clic(pagina.$('a.portal-campana'));
  await esperar();
  assert.match(pagina.texto(), /No tienes notificaciones/);
});

test('si el servidor falla, avisa con un mensaje', async () => {
  pagina.servidor['GET /api/notificaciones'] = respuesta(500, { message: 'Error' });
  pagina.alertas.length = 0;
  clic(pagina.$('a[href="consumidor.html"]'));
  await esperar();
  clic(pagina.$('a.portal-campana'));
  await esperar();
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudieron cargar las notificaciones'));
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
