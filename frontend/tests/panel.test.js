// Piezas compartidas de los paneles (app/panel.js), probadas directamente sobre la página de empresa.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, clic } from './navegador.js';

const pagina = abrirPagina('empresa.html');
const panel = await import('../app/panel.js');
const $ = pagina.$;

test('icono crea un SVG decorativo con el trazo pedido', () => {
  const svg = panel.icono('pack');
  assert.equal(svg.getAttribute('viewBox'), '0 0 24 24');
  assert.equal(svg.getAttribute('aria-hidden'), 'true');
  assert.match(svg.querySelector('path').getAttribute('d'), /^M4 8h16v13H4z/);
});

test('pintarIconos coloca un icono al inicio de cada elemento con data-icono', () => {
  panel.pintarIconos();
  for (const zona of pagina.$$('[data-icono]')) assert.equal(zona.firstChild.nodeName, 'svg');
});

test('crearNavegacion muestra la vista pedida, marca su botón y avisa con sus datos', () => {
  const vistas = [];
  const mostrar = panel.crearNavegacion((vista, datos) => vistas.push([vista, datos]));
  mostrar('locales:detalle', 5);
  assert.equal($('[data-panel-vista="locales:detalle"]').hidden, false);
  assert.equal($('[data-panel-vista="resumen"]').hidden, true);
  assert.equal($('[data-vista="locales"]').getAttribute('aria-current'), 'page');
  assert.equal($('[data-vista="resumen"]').hasAttribute('aria-current'), false);
  clic($('[data-vista="avisos"]'));
  assert.equal($('[data-panel-vista="avisos"]').hidden, false);
  assert.deepEqual(vistas, [['locales:detalle', 5], ['avisos', undefined]]);
});

test('crearNavegacion funciona sin función de aviso', () => {
  const mostrar = panel.crearNavegacion();
  mostrar('resumen');
  assert.equal($('[data-panel-vista="resumen"]').hidden, false);
});

test('mensaje de éxito: texto verde y alerta «Listo» por defecto', () => {
  pagina.alertas.length = 0;
  panel.mensaje('Datos guardados');
  assert.equal($('#mensaje').textContent, 'Datos guardados');
  assert.equal($('#mensaje').className, 'ok');
  assert.deepEqual([pagina.ultimaAlerta().icon, pagina.ultimaAlerta().title, pagina.ultimaAlerta().text], ['success', 'Listo', 'Datos guardados']);
  panel.mensaje('Cuenta creada correctamente', false, 'Cuenta creada');
  assert.equal(pagina.ultimaAlerta().title, 'Cuenta creada');
});

test('mensaje de error: texto rojo y alerta de error con su título', () => {
  pagina.alertas.length = 0;
  panel.mensaje('El local no existe', true, 'No se pudo cargar');
  assert.equal($('#mensaje').className, 'error');
  assert.deepEqual([pagina.ultimaAlerta().icon, pagina.ultimaAlerta().title], ['error', 'No se pudo cargar']);
});

test('mensaje con un fallo de red muestra «Sin conexión»', () => {
  panel.mensaje('Failed to fetch', true);
  assert.equal($('#mensaje').textContent, 'Sin conexión. Revisa tu internet e intenta de nuevo.');
  assert.equal(pagina.ultimaAlerta().title, 'Sin conexión');
});

test('mensaje vacío limpia la zona y no abre alertas', () => {
  pagina.alertas.length = 0;
  panel.mensaje('');
  assert.equal($('#mensaje').textContent, '');
  assert.equal($('#mensaje').className, '');
  assert.deepEqual(pagina.alertas, []);
});

test('mensaje sin zona #mensaje solo abre la alerta', () => {
  const zona = $('#mensaje');
  zona.remove();
  try {
    panel.mensaje('Hecho', false, 'Listo');
    assert.equal(pagina.ultimaAlerta().text, 'Hecho');
  } finally { $('main').prepend(zona); }
});

test('estadoEn escribe el estado del formulario y solo alerta si es un error', () => {
  const zona = pagina.document.createElement('p');
  pagina.alertas.length = 0;
  panel.estadoEn(zona, 'Guardado');
  assert.equal(zona.className, 'estado-formulario ok');
  assert.deepEqual(pagina.alertas, []);
  panel.estadoEn(zona, 'Ingresa un RUC válido', true, 'Revisa el RUC');
  assert.equal(zona.className, 'estado-formulario error');
  assert.deepEqual([pagina.ultimaAlerta().icon, pagina.ultimaAlerta().title], ['warning', 'Revisa el RUC']);
  panel.estadoEn(zona, 'NetworkError when attempting to fetch resource.', true);
  assert.equal(zona.textContent, 'Sin conexión. Revisa tu internet e intenta de nuevo.');
});

test('insignia, banda y vacío arman sus bloques con el tono indicado', () => {
  assert.equal(panel.insignia('Activo').className, 'insignia');
  assert.equal(panel.insignia('Activo', 'ok').className, 'insignia ok');
  const info = panel.banda('info', 'Para saber', null);
  assert.equal(info.getAttribute('role'), 'status');
  assert.equal(info.querySelector('.banda-icono').textContent, 'i');
  assert.equal(info.querySelector('p'), null);
  const peligro = panel.banda('peligro', 'Cuidado', 'Detalle', pagina.document.createElement('button'));
  assert.equal(peligro.getAttribute('role'), 'alert');
  assert.equal(peligro.querySelector('.banda-icono').textContent, '!');
  assert.ok(peligro.querySelector('button'));
  const vacioSimple = panel.vacio('Nada por aquí');
  assert.equal(vacioSimple.textContent, 'Nada por aquí');
  assert.equal(panel.vacio('Sin datos', 'Agrega uno', panel.insignia('x')).querySelectorAll('p').length, 2);
});

test('cifra y cifraImpacto muestran etiqueta, valor y detalle', () => {
  const simple = panel.cifra('Ventas', '3');
  assert.equal(simple.className, 'cifra');
  assert.equal(simple.querySelector('small'), null);
  const impacto = panel.cifraImpacto('CO₂ evitado', '2,5 kg', 'Estimado');
  assert.equal(impacto.className, 'cifra impacto');
  assert.equal(impacto.querySelector('small').textContent, 'Estimado');
  assert.equal(panel.cifra('Total', '9', '', true).className, 'cifra destacada');
});

test('tabla pone encabezados, celdas con nodos o texto y un pie opcional', () => {
  const enlace = pagina.document.createElement('a');
  const contenedor = panel.tabla([['Producto'], ['Monto', 'numero']], [['Pack', '10'], [enlace, null]], ['Total', undefined]);
  assert.deepEqual([...contenedor.querySelectorAll('th')].map(th => [th.textContent, th.className, th.scope]),
    [['Producto', '', 'col'], ['Monto', 'numero', 'col']]);
  const celdas = [...contenedor.querySelectorAll('tbody td')];
  assert.equal(celdas[1].className, 'numero');
  assert.equal(celdas[2].firstChild, enlace);
  assert.equal(celdas[3].textContent, '—');
  assert.deepEqual([...contenedor.querySelectorAll('tfoot td')].map(td => td.textContent), ['Total', '']);
  assert.equal(panel.tabla([['A']], []).querySelector('tfoot'), null);
});

test('fecha, hora y rangoHorario formatean en español de Perú', () => {
  assert.equal(panel.fecha(null), '—');
  assert.equal(panel.fecha('2026-11-27'), new Date(2026, 10, 27).toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' }));
  assert.match(panel.fecha('2026-11-27T23:30:00'), /27/);
  assert.equal(panel.hora('2026-11-27T08:05:00'), '08:05');
  const mismoDia = panel.rangoHorario('2026-11-27T18:00:00', '2026-11-27T20:30:00');
  assert.match(mismoDia, / · 18:00 – 20:30$/);
  const otroDia = panel.rangoHorario('2026-11-27T22:00:00', '2026-11-28T01:00:00');
  assert.match(otroDia, /22:00 – .*28.* 01:00$/);
  assert.doesNotMatch(otroDia, /·/);
});

test('insigniaLocal y las tablas de nombres traducen los estados', () => {
  assert.equal(panel.insigniaLocal('SUSPENDIDO').className, 'insignia peligro');
  assert.equal(panel.insigniaLocal('INACTIVO').textContent, 'Inactivo');
  assert.equal(panel.insigniaLocal('DESCONOCIDO').textContent, 'DESCONOCIDO');
  assert.equal(panel.tiposDocumento.CERTIFICADO_SANITARIO, 'Certificado sanitario');
  assert.deepEqual(panel.vigencias.VENCIDO, ['Vencido', 'peligro']);
  assert.deepEqual(panel.estadosLocal.ACTIVO, ['Activo', 'ok']);
});

test('filasDieta marca lo apto y lo no apto', () => {
  const dieta = panel.filasDieta(['Apto para veganos']);
  const filas = [...dieta.querySelectorAll('p')];
  assert.deepEqual(filas.map(f => f.className), ['no-apto', 'apto', 'no-apto']);
  assert.match(filas[1].textContent, /^✓Apto para veganos$/);
  assert.match(filas[0].textContent, /^✕No apto para vegetarianos$/);
  assert.equal(panel.filasDieta().querySelectorAll('.apto').length, 0);
});

test('listaAvisos muestra los avisos y llama a las acciones de lectura', () => {
  const leidos = [];
  let todas = 0;
  const avisos = [
    { id: 1, titulo: 'Pedido listo', mensaje: 'Recógelo hoy', fecha: '2026-11-27T10:00:00', leida: false },
    { id: 2, titulo: 'Bienvenida', mensaje: 'Gracias por unirte', fecha: '2026-11-20T10:00:00', leida: true }
  ];
  const lista = panel.listaAvisos(avisos, { alLeer: aviso => leidos.push(aviso.id), alLeerTodas: () => { todas++; } });
  assert.equal(lista.querySelectorAll('.aviso-item').length, 2);
  assert.equal(lista.querySelectorAll('.aviso-item.no-leida').length, 1);
  const botones = [...lista.querySelectorAll('button')];
  assert.deepEqual(botones.map(b => b.textContent), ['Marcar todas como leídas', 'Marcar como leída']);
  clic(botones[1]);
  clic(botones[0]);
  assert.deepEqual(leidos, [1]);
  assert.equal(todas, 1);
  const leidas = panel.listaAvisos([avisos[1]], { alLeer() {}, alLeerTodas() {} });
  assert.equal(leidas.querySelector('button'), null);
  assert.match(panel.listaAvisos([], {}).textContent, /No tienes notificaciones/);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
