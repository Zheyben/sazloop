// Portal de empresa de un usuario que todavía no registró su empresa (primer paso del propietario).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, llenarYEnviar, respuesta } from './navegador.js';

const PROPIETARIO = { id: 8, nombres: 'Jorge', apellidos: 'Salas', email: 'jorge@sazloop.test', tieneFoto: false };
const INDICADORES_VACIOS = {
  establecimientos: [],
  total: { establecimientoId: null, establecimiento: 'Total', ventas: 0, importeVentas: 0, comisiones: 0, ingresos: 0,
    cancelaciones: 0, noRecogidas: 0, cantidadResenas: 0, calificacionPromedio: 0, packsEntregados: 0,
    alimentoRecuperadoKg: 0, co2EvitadoKg: 0, resenasRecientes: [] }
};

let locales = [];
const servidor = {
  'GET /api/sesion': PROPIETARIO,
  'GET /api/empresas/actual': respuesta(204),
  'GET /api/notificaciones': [],
  'GET /api/empresas/actual/indicadores': INDICADORES_VACIOS,
  'GET /api/empresas/actual/establecimientos': () => locales
};
const pagina = abrirPagina('empresa.html', { servidor });
await import('../app/empresa.js');
await esperar();

const $ = pagina.$;
const vistaVisible = () => pagina.$$('[data-panel-vista]').filter(s => !s.hidden).map(s => s.dataset.panelVista);
const boton = (texto, raiz = pagina.document) => [...raiz.querySelectorAll('button')].find(b => b.textContent.trim() === texto);

test('sin empresa muestra solo el formulario de registro, sin menú lateral', () => {
  assert.equal($('#nombre-usuario').textContent, 'Jorge');
  assert.deepEqual(vistaVisible(), ['registro']);
  assert.equal($('.panel-lateral').hidden, true);
  assert.equal(pagina.llamadasA('GET', '/api/notificaciones').length, 0);
});

test('al volver a la pestaña durante el registro no se consulta nada', async () => {
  const antes = pagina.llamadas.length;
  pagina.document.dispatchEvent(new pagina.window.Event('visibilitychange'));
  await esperar();
  assert.equal(pagina.llamadas.length, antes);
});

test('si el RUC ya está registrado, se avisa en el formulario', async () => {
  servidor['POST /api/empresas'] = respuesta(409, { message: 'Ya existe una empresa con ese RUC' });
  llenarYEnviar($('#form-empresa'), { razonSocial: 'Salas Foods S.A.C.', nombreComercial: 'Salas Foods', ruc: '20987654321' });
  await esperar();
  const estado = $('#form-empresa .estado-formulario');
  assert.equal(estado.textContent, 'Ya existe una empresa con ese RUC');
  assert.equal(estado.className, 'estado-formulario error');
  assert.equal(pagina.ultimaAlerta().title, 'No se pudo registrar la empresa');
  assert.deepEqual(vistaVisible(), ['registro']);
});

test('registrar la empresa abre «Mis locales» con el formulario del primer local', async () => {
  servidor['POST /api/empresas'] = cuerpo => respuesta(201, { id: 4, ...cuerpo });
  pagina.alertas.length = 0;
  llenarYEnviar($('#form-empresa'), { razonSocial: 'Salas Foods S.A.C.', nombreComercial: 'Salas Foods', ruc: '20987654321' });
  await esperar();
  assert.deepEqual(pagina.llamadasA('POST', '/api/empresas').at(-1).cuerpo,
    { razonSocial: 'Salas Foods S.A.C.', nombreComercial: 'Salas Foods', ruc: '20987654321' });
  assert.equal($('#titulo-panel').textContent, 'Salas Foods');
  assert.equal($('.panel-lateral').hidden, false);
  assert.deepEqual(vistaVisible(), ['locales']);
  assert.match($('#lista-locales').textContent, /Aún no registras locales/);
  assert.equal($('#form-local').hidden, false);
  assert.ok(pagina.alertas.some(a => a.title === 'Empresa registrada'));
});

test('si el navegador no permite la ubicación, lo explica', async () => {
  Object.defineProperty(pagina.window.navigator, 'geolocation', { configurable: true, value: undefined });
  clic($('#usar-ubicacion'));
  await esperar();
  assert.equal($('#estado-ubicacion').textContent, 'Tu navegador no permite obtener la ubicación.');
  assert.equal($('#estado-ubicacion').className, 'ubicacion-estado error');
});

test('el resumen sin locales invita a registrar el primero', async () => {
  clic($('.panel-lateral [data-vista="resumen"]'));
  await esperar();
  const detalle = $('#detalle-establecimientos');
  assert.match(detalle.textContent, /Aún no tienes locales/);
  assert.equal($('#alertas-resumen').textContent, '');
  assert.equal([...$('#cifras-empresa').querySelectorAll('.cifra strong')].map(s => s.textContent).join('|'),
    '0|S/ 0.00|S/ 0.00|0|0|—|0|0,0 kg|0,0 kg');
  clic($('.panel-lateral [data-vista="locales"]'));
  await esperar();
  clic($('#cancelar-local'));
  clic($('.panel-lateral [data-vista="resumen"]'));
  await esperar();
  clic(boton('Agregar local', detalle));
  await esperar();
  assert.deepEqual(vistaVisible(), ['locales']);
  assert.equal($('#form-local').hidden, false);
});

test('el resumen avisa en singular de un local pendiente y uno aprobado sin cuenta', async () => {
  locales = [
    { id: 20, nombre: 'Salas Centro', distrito: 'Cercado de Lima', direccion: 'Jr. de la Unión 300', estado: 'PENDIENTE_APROBACION', cuentaTrabajador: null, latitud: null, longitud: null },
    { id: 21, nombre: 'Salas Pueblo Libre', distrito: 'Pueblo Libre', direccion: 'Av. Bolívar 800', estado: 'ACTIVO', cuentaTrabajador: null, latitud: null, longitud: null },
    { id: 22, nombre: 'Salas Antiguo', distrito: 'Rímac', direccion: 'Jr. Trujillo 10', estado: 'INACTIVO', cuentaTrabajador: null, latitud: null, longitud: null }
  ];
  clic($('.panel-lateral [data-vista="resumen"]'));
  await esperar();
  const alertas = $('#alertas-resumen').textContent;
  assert.match(alertas, /1 local espera aprobación/);
  assert.match(alertas, /Salas Pueblo Libre ya fue aprobado/);
});

test('un local con la renovación en revisión la muestra sin formulario', async () => {
  servidor['GET /api/establecimientos/21/documentacion'] = {
    establecimientoId: 21, establecimiento: 'Salas Pueblo Libre', estado: 'ACTIVO', motivoSuspension: null, suspendidoDesde: null,
    documentos: [{ id: 1, tipo: 'LICENCIA_FUNCIONAMIENTO', numero: 'LF-1', vencimiento: '2026-12-01', vigencia: 'POR_VENCER' }],
    tiposVencidos: [], tiposRenovables: ['LICENCIA_FUNCIONAMIENTO'],
    solicitud: { id: 5, establecimientoId: 21, estado: 'EN_REVISION', renovacion: true, motivoDecision: null, fechaSolicitud: '2026-11-25T09:00:00',
      documentos: [{ id: 9, tipo: 'LICENCIA_FUNCIONAMIENTO', numero: 'LF-2', fechaEmision: '2026-11-20', fechaVencimiento: '2027-11-20' }] }
  };
  clic($('.panel-lateral [data-vista="locales"]'));
  await esperar();
  const tarjeta = pagina.$$('#lista-locales .item-panel').find(t => t.querySelector('h3').textContent === 'Salas Pueblo Libre');
  clic(boton('Gestionar', tarjeta));
  await esperar();
  const detalle = $('[data-panel-vista="locales:detalle"]');
  assert.match(detalle.textContent, /Renovación en revisión/);
  assert.match(detalle.textContent, /LF-2/);
  assert.equal(detalle.querySelector('input[name="archivo"]'), null);
  assert.ok(detalle.querySelector('input[name="nombreUsuario"]'), 'un local activo sin cuenta puede crearla');
});

test('un local inactivo no permite crear la cuenta del trabajador', async () => {
  servidor['GET /api/establecimientos/22/documentacion'] = {
    establecimientoId: 22, establecimiento: 'Salas Antiguo', estado: 'INACTIVO', motivoSuspension: null, suspendidoDesde: null,
    documentos: [], tiposVencidos: [], tiposRenovables: [], solicitud: null
  };
  clic(boton('← Mis locales'));
  await esperar();
  const tarjeta = pagina.$$('#lista-locales .item-panel').find(t => t.querySelector('h3').textContent === 'Salas Antiguo');
  assert.match(tarjeta.textContent, /Inactivo/);
  clic(boton('Gestionar', tarjeta));
  await esperar();
  const detalle = $('[data-panel-vista="locales:detalle"]');
  assert.match(detalle.textContent, /Solicitud de afiliación/);
  assert.match(detalle.textContent, /Podrás crear la cuenta del trabajador cuando Sazloop apruebe el local\./);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
