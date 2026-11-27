// Panel de administración de la plataforma (app/administrador.html + administrador.js).
// Una sola carga de la página: revisa solicitudes, decide, mira los locales y cierra sesión.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, respuesta } from './navegador.js';

const doc = (id, tipo, extra = {}) => ({ id, tipo, numero: `N-${id}`, emision: '2026-01-10', vencimiento: '2030-01-10', resultado: null, observaciones: null, ...extra });
const base = {
  establecimientoId: 3, establecimiento: 'Aurora San Borja', estado: 'EN_REVISION', renovacion: false,
  estadoLocal: 'PENDIENTE_APROBACION', empresa: 'Aurora Cocina', ruc: '20999990001', distrito: 'San Borja',
  direccion: 'Av. San Luis 1200', fechaSolicitud: '2026-11-20T10:00:00', motivoDecision: null
};

const SOLICITUDES = {
  5: { ...base, id: 5, documentos: [
    doc(51, 'LICENCIA_FUNCIONAMIENTO', { resultado: 'APROBADO', observaciones: 'Conforme' }),
    doc(52, 'CERTIFICADO_SANITARIO'),
    doc(53, 'INSPECCION_SEGURIDAD', { vencimiento: '2020-05-01' })
  ] },
  6: { ...base, id: 6, establecimientoId: 2, establecimiento: 'Aurora Miraflores', renovacion: true, estadoLocal: 'SUSPENDIDO',
    distrito: 'Miraflores', direccion: 'Av. Larco 345', documentos: [doc(61, 'CERTIFICADO_SANITARIO')] },
  7: { ...base, id: 7, establecimientoId: 4, establecimiento: 'Bodega Lince', empresa: 'Bodega Lince S.A.C.', ruc: '20111111111',
    documentos: [doc(71, 'LICENCIA_FUNCIONAMIENTO'), doc(72, 'OTRO', { vencimiento: null })] },
  8: { ...base, id: 8, establecimiento: 'Café Barranco', estado: 'RECHAZADA', motivoDecision: 'Licencia ilegible',
    documentos: [doc(81, 'LICENCIA_FUNCIONAMIENTO', { resultado: 'RECHAZADO', observaciones: 'Ilegible' }), doc(82, 'PERMISO_MUNICIPAL')] },
  9: { ...base, id: 9, establecimiento: 'Panadería Surco', estado: 'APROBADA', documentos: [doc(91, 'CERTIFICADO_SANITARIO', { resultado: 'APROBADO' })] }
};
let pendientes = [SOLICITUDES[5], SOLICITUDES[6]];

const LOCALES = [
  { id: 1, nombre: 'Aurora Miraflores', empresa: 'Aurora Cocina', ruc: '20999990001', distrito: 'Miraflores', direccion: 'Av. Larco 345',
    estado: 'SUSPENDIDO', motivoSuspension: 'Certificado sanitario vencido', suspendidoDesde: '2026-11-09T00:00:00', suspensionAutomatica: true, solicitudEnRevision: true },
  { id: 2, nombre: 'Aurora San Borja', empresa: 'Aurora Cocina', ruc: '20999990001', distrito: 'San Borja', direccion: 'Av. San Luis 1200',
    estado: 'PENDIENTE_APROBACION', motivoSuspension: null, suspendidoDesde: null, suspensionAutomatica: false, solicitudEnRevision: true },
  { id: 3, nombre: 'Bodega Lince', empresa: 'Bodega Lince S.A.C.', ruc: '20111111111', distrito: 'Lince', direccion: 'Jr. Risso 200',
    estado: 'ACTIVO', motivoSuspension: null, suspendidoDesde: null, suspensionAutomatica: false, solicitudEnRevision: false },
  { id: 4, nombre: 'Café Chorrillos', empresa: 'Café SAC', ruc: '20222222222', distrito: 'Chorrillos', direccion: 'Av. Huaylas 10',
    estado: 'SUSPENDIDO', motivoSuspension: null, suspendidoDesde: '2026-10-01T00:00:00', suspensionAutomatica: false, solicitudEnRevision: false }
];

const pagina = abrirPagina('administrador.html', {
  servidor: {
    'GET /api/administradores/sesion': { id: 1, nombre: 'Ana Torres', email: 'admin@sazloop.test' },
    'GET /api/administradores/solicitudes': () => pendientes,
    'GET /api/administradores/solicitudes/5': SOLICITUDES[5],
    'GET /api/administradores/solicitudes/6': SOLICITUDES[6],
    'GET /api/administradores/solicitudes/7': SOLICITUDES[7],
    'GET /api/administradores/solicitudes/8': SOLICITUDES[8],
    'GET /api/administradores/solicitudes/9': SOLICITUDES[9],
    'GET /api/administradores/establecimientos': LOCALES,
    'DELETE /api/administradores/sesion': respuesta(204)
  }
});
await import('../app/administrador.js');
await esperar();

const titulos = () => pagina.alertas.map(a => a.title);
const detalle = () => pagina.$('[data-panel-vista="solicitudes:detalle"]');
const tarjetas = () => pagina.$$('#lista-solicitudes .item-panel');
const boton = (zona, texto) => [...zona.querySelectorAll('button')].find(b => b.textContent.trim() === texto);
async function revisar(establecimiento) {
  const tarjeta = tarjetas().find(t => t.querySelector('h3').textContent === establecimiento);
  clic(boton(tarjeta, 'Revisar'));
  await esperar();
}
const formulario = () => detalle().querySelector('form');
const tarjetaDoc = titulo => [...detalle().querySelectorAll('.documento-revision')].find(t => t.querySelector('h4').textContent === titulo);
function silenciarConsola() {
  const original = console.error;
  console.error = () => {};
  return () => { console.error = original; };
}

// ---------- Solicitudes pendientes ----------

test('muestra el nombre del administrador y las solicitudes pendientes', () => {
  assert.equal(pagina.$('#nombre-admin').textContent, 'Ana Torres');
  assert.match(pagina.texto(), /Revisión de locales/);
  assert.match(pagina.texto(), /Solicitudes pendientes/);
  assert.equal(pagina.$('#contador-solicitudes').textContent, '2');
  const [nueva, renovacion] = tarjetas();
  assert.match(nueva.textContent, /Afiliación de local nuevo/);
  assert.match(nueva.textContent, /Aurora San Borja/);
  assert.match(nueva.textContent, /Aurora Cocina · RUC 20999990001/);
  assert.match(nueva.textContent, /Av\. San Luis 1200, San Borja/);
  assert.match(nueva.textContent, /1 de 3 documentos revisados/);
  assert.match(nueva.textContent, /Pendiente de aprobación/);
  assert.ok(boton(nueva, 'Revisar'));
  assert.match(renovacion.textContent, /Renovación de documentos/);
  assert.match(renovacion.textContent, /Suspendido/);
  assert.match(renovacion.textContent, /0 de 1 documentos revisados/);
});

test('«Revisar» abre la solicitud de afiliación con sus documentos', async () => {
  await revisar('Aurora San Borja');
  assert.equal(pagina.llamadasA('GET', '/api/administradores/solicitudes/5').length, 1);
  assert.equal(detalle().hidden, false);
  assert.equal(pagina.$('[data-panel-vista="solicitudes"]').hidden, true);
  const texto = detalle().textContent;
  assert.match(texto, /SOLICITUD DE AFILIACIÓN/);
  assert.match(texto, /Aurora San Borja/);
  assert.match(texto, /Aurora Cocina · RUC 20999990001 · Av\. San Luis 1200, San Borja/);
  assert.doesNotMatch(texto, /Al aprobarla, estos documentos reemplazan/);
  // Documento ya revisado: solo lectura.
  const licencia = tarjetaDoc('Licencia de funcionamiento');
  assert.match(licencia.textContent, /Aprobado/);
  assert.match(licencia.textContent, /Observaciones: Conforme/);
  assert.equal(licencia.querySelector('input'), null);
  assert.equal(licencia.querySelector('a').getAttribute('href'), '/api/administradores/documentos/51/archivo');
  // Documento por revisar: se aprueba por defecto.
  const certificado = tarjetaDoc('Certificado sanitario');
  assert.match(certificado.textContent, /Por revisar/);
  assert.match(certificado.textContent, /N-52/);
  assert.equal(certificado.querySelector('input[value="APROBADO"]').checked, true);
  assert.equal(certificado.querySelector('a').getAttribute('href'), '/api/administradores/documentos/52/archivo');
  // Documento vencido: solo puede rechazarse.
  const inspeccion = tarjetaDoc('Inspección de seguridad');
  assert.match(inspeccion.textContent, /Vencido/);
  assert.match(inspeccion.textContent, /\(vencido\)/);
  assert.match(inspeccion.textContent, /Un documento vencido solo puede rechazarse/);
  assert.equal(inspeccion.querySelector('input[value="APROBADO"]').disabled, true);
  assert.equal(inspeccion.querySelector('input[value="RECHAZADO"]').checked, true);
  assert.match(texto, /Se rechazará la solicitud porque hay documentos rechazados\./);
});

test('rechazar un documento sin motivo pide indicarlo y no guarda', async () => {
  pagina.alertas.length = 0;
  formulario().requestSubmit();
  await esperar();
  assert.match(detalle().querySelector('.estado-formulario').textContent, /Indica por qué se rechaza: Inspección de seguridad\./);
  assert.ok(titulos().includes('Falta el motivo'));
  assert.equal(pagina.document.activeElement, tarjetaDoc('Inspección de seguridad').querySelector('textarea'));
  assert.equal(pagina.llamadasA('POST', '/api/administradores/solicitudes/5/resolucion').length, 0);
});

test('si el servidor falla al guardar la decisión, lo muestra y permite reintentar', async () => {
  tarjetaDoc('Inspección de seguridad').querySelector('textarea').value = 'Inspección vencida en mayo de 2020';
  detalle().querySelectorAll('textarea')[2].value = '  Renueve la inspección y vuelva a enviar  ';
  pagina.servidor['POST /api/administradores/solicitudes/5/resolucion'] = respuesta(500, { message: 'Error' });
  pagina.alertas.length = 0;
  const restaurar = silenciarConsola();
  try {
    formulario().requestSubmit();
    await esperar();
  } finally { restaurar(); }
  assert.ok(titulos().includes('No se pudo guardar la decisión'));
  assert.match(detalle().querySelector('.estado-formulario').textContent, /error inesperado en el servidor \(código 500\)/);
  assert.equal(boton(detalle(), 'Guardar decisión').disabled, false);
});

test('rechazar la solicitud envía cada documento con su motivo y vuelve a la lista', async () => {
  pagina.servidor['POST /api/administradores/solicitudes/5/resolucion'] = () => { pendientes = [SOLICITUDES[6]]; return { ...SOLICITUDES[5], estado: 'RECHAZADA' }; };
  pagina.alertas.length = 0;
  formulario().requestSubmit();
  await esperar();
  const llamadas = pagina.llamadasA('POST', '/api/administradores/solicitudes/5/resolucion');
  assert.deepEqual(llamadas.at(-1).cuerpo, {
    documentos: [
      { id: 52, resultado: 'APROBADO', observaciones: null },
      { id: 53, resultado: 'RECHAZADO', observaciones: 'Inspección vencida en mayo de 2020' }
    ],
    comentario: 'Renueve la inspección y vuelva a enviar'
  });
  assert.ok(titulos().includes('Solicitud rechazada'));
  assert.equal(pagina.$('#mensaje').textContent, 'Solicitud rechazada. El propietario recibirá el motivo.');
  assert.equal(pagina.$('[data-panel-vista="solicitudes"]').hidden, false);
  assert.equal(tarjetas().length, 1);
  assert.equal(pagina.$('#contador-solicitudes').textContent, '1');
});

test('la renovación muestra su aviso y cambia la decisión al marcar los documentos', async () => {
  await revisar('Aurora Miraflores');
  const texto = detalle().textContent;
  assert.match(texto, /RENOVACIÓN DE DOCUMENTOS/);
  assert.match(texto, /Al aprobarla, estos documentos reemplazan a los anteriores del mismo tipo/);
  assert.match(texto, /Se aprobará la renovación del local\./);
  const rechazar = detalle().querySelector('input[value="RECHAZADO"]');
  rechazar.checked = true;
  rechazar.dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
  assert.match(detalle().textContent, /Se rechazará la renovación porque hay documentos rechazados\./);
  const aprobar = detalle().querySelector('input[value="APROBADO"]');
  aprobar.checked = true;
  aprobar.dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
  assert.match(detalle().textContent, /Se aprobará la renovación del local\./);
});

test('aprobar la renovación avisa que el propietario fue notificado', async () => {
  pagina.servidor['POST /api/administradores/solicitudes/6/resolucion'] = () => { pendientes = []; return { ...SOLICITUDES[6], estado: 'APROBADA' }; };
  pagina.alertas.length = 0;
  formulario().requestSubmit();
  await esperar();
  assert.deepEqual(pagina.llamadasA('POST', '/api/administradores/solicitudes/6/resolucion')[0].cuerpo, {
    documentos: [{ id: 61, resultado: 'APROBADO', observaciones: null }], comentario: null
  });
  assert.ok(titulos().includes('Decisión guardada'));
  assert.equal(pagina.$('#mensaje').textContent, 'Renovación aprobada. El propietario fue notificado.');
});

test('sin solicitudes muestra el estado vacío y oculta el contador', () => {
  assert.match(pagina.$('#lista-solicitudes').textContent, /No hay solicitudes pendientes/);
  assert.equal(pagina.$('#contador-solicitudes').textContent, '');
});

test('aprobar un local nuevo indica que ya puede crear la cuenta del trabajador', async () => {
  pendientes = [SOLICITUDES[7], SOLICITUDES[8], SOLICITUDES[9]];
  clic(pagina.$('.panel-lateral [data-vista="solicitudes"]'));
  await esperar();
  assert.equal(tarjetas().length, 3);
  await revisar('Bodega Lince');
  assert.match(detalle().textContent, /Se aprobará el local\./);
  assert.match(tarjetaDoc('Otro documento').textContent, /Vencimiento—/);
  pagina.servidor['POST /api/administradores/solicitudes/7/resolucion'] = { ...SOLICITUDES[7], estado: 'APROBADA' };
  pagina.alertas.length = 0;
  formulario().requestSubmit();
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/administradores/solicitudes/7/resolucion')[0].cuerpo.documentos.length, 2);
  assert.ok(titulos().includes('Decisión guardada'));
  assert.equal(pagina.$('#mensaje').textContent, 'Local aprobado. El propietario ya puede crear la cuenta del trabajador.');
});

test('una solicitud ya resuelta se muestra en modo lectura con su motivo', async () => {
  await revisar('Café Barranco');
  const texto = detalle().textContent;
  assert.equal(formulario(), null);
  assert.match(texto, /Solicitud resuelta/);
  assert.match(texto, /Motivo: Licencia ilegible/);
  assert.match(tarjetaDoc('Licencia de funcionamiento').textContent, /Rechazado/);
  assert.match(tarjetaDoc('Licencia de funcionamiento').textContent, /Observaciones: Ilegible/);
  assert.match(tarjetaDoc('PERMISO_MUNICIPAL').textContent, /Sin revisar/);
});

test('«← Solicitudes» vuelve a la lista', async () => {
  clic(boton(detalle(), '← Solicitudes'));
  await esperar();
  assert.equal(pagina.$('[data-panel-vista="solicitudes"]').hidden, false);
  assert.equal(detalle().hidden, true);
});

test('una solicitud aprobada sin motivo solo indica que está resuelta', async () => {
  await revisar('Panadería Surco');
  assert.match(detalle().textContent, /Solicitud resuelta/);
  assert.doesNotMatch(detalle().textContent, /Motivo:/);
  assert.match(tarjetaDoc('Certificado sanitario').textContent, /Aprobado/);
});

test('si no se puede abrir una solicitud, avisa con un mensaje', async () => {
  pagina.servidor['GET /api/administradores/solicitudes/9'] = respuesta(404, { message: 'La solicitud no existe' });
  clic(boton(detalle(), '← Solicitudes'));
  await esperar();
  pagina.alertas.length = 0;
  await revisar('Panadería Surco');
  assert.ok(titulos().includes('No se pudo cargar la información'));
  assert.equal(pagina.$('#mensaje').textContent, 'La solicitud no existe');
});

// ---------- Establecimientos ----------

const filas = () => pagina.$$('#tabla-establecimientos tbody tr');
function filtrar(valor) {
  const opcion = pagina.$(`#filtro-estado input[value="${valor}"]`);
  opcion.checked = true;
  opcion.dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
}

test('«Establecimientos» lista todos los locales con su estado', async () => {
  clic(pagina.$('.panel-lateral [data-vista="establecimientos"]'));
  await esperar();
  assert.equal(pagina.$('[data-panel-vista="establecimientos"]').hidden, false);
  assert.equal(filas().length, 4);
  const texto = pagina.$('#tabla-establecimientos').textContent;
  assert.match(texto, /Aurora Miraflores/);
  assert.match(texto, /Aurora Cocina · RUC 20999990001/);
  assert.match(texto, /Certificado sanitario vencido · desde el .+ \(suspensión automática\)/);
  assert.match(texto, /Solicitud en revisión/);
  assert.match(texto, /Suspendido · desde el /);
  assert.match(filas()[2].textContent, /Bodega Lince.*Activo.*—/);
});

test('el filtro muestra solo los locales del estado elegido', () => {
  filtrar('SUSPENDIDO');
  assert.equal(filas().length, 2);
  assert.ok(filas().every(f => /Suspendido/.test(f.textContent)));
  filtrar('ACTIVO');
  assert.equal(filas().length, 1);
  assert.match(filas()[0].textContent, /Bodega Lince/);
});

test('sin locales del estado elegido muestra el estado vacío', async () => {
  pagina.servidor['GET /api/administradores/establecimientos'] = LOCALES.filter(l => l.estado !== 'PENDIENTE_APROBACION');
  filtrar('PENDIENTE_APROBACION');
  assert.equal(filas().length, 1, 'el filtro usa la lista ya cargada');
  clic(pagina.$('.panel-lateral [data-vista="establecimientos"]'));
  await esperar();
  assert.match(pagina.$('#tabla-establecimientos').textContent, /No hay locales con ese estado/);
  filtrar('');
  assert.equal(filas().length, 3);
});

test('si falla la carga de los locales, avisa con un mensaje', async () => {
  pagina.servidor['GET /api/administradores/establecimientos'] = respuesta(403, { message: 'Forbidden' });
  pagina.alertas.length = 0;
  clic(pagina.$('.panel-lateral [data-vista="establecimientos"]'));
  await esperar();
  assert.ok(titulos().includes('No se pudo cargar la información'));
  assert.equal(pagina.$('#mensaje').textContent, 'No tienes permiso para esta acción.');
});

test('si falla la carga de las solicitudes, avisa con un mensaje', async () => {
  pagina.servidor['GET /api/administradores/solicitudes'] = new TypeError('Failed to fetch');
  pagina.alertas.length = 0;
  clic(pagina.$('.panel-lateral [data-vista="solicitudes"]'));
  await esperar();
  assert.deepEqual(titulos(), ['Sin conexión']);
  assert.match(pagina.$('#mensaje').textContent, /Sin conexión/);
});

// ---------- Cerrar sesión ----------

test('cerrar sesión borra la sesión y vuelve al acceso de administración', async () => {
  clic(pagina.$('#salir'));
  await esperar();
  assert.equal(pagina.llamadasA('DELETE', '/api/administradores/sesion').length, 1);
  assert.deepEqual(pagina.navegaciones, ['acceso-administrador.html']);
});

test('cerrar sesión aunque la sesión ya no exista también vuelve al acceso', async () => {
  pagina.servidor['DELETE /api/administradores/sesion'] = new TypeError('Failed to fetch');
  clic(pagina.$('#salir'));
  await esperar();
  assert.deepEqual(pagina.navegaciones, ['acceso-administrador.html', 'acceso-administrador.html']);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
