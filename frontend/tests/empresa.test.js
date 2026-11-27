// Portal de empresa (app/empresa.html + empresa.js) con un propietario que ya tiene varios locales.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, llenarYEnviar, respuesta } from './navegador.js';

const PROPIETARIO = { id: 3, nombres: '  Rosa María ', apellidos: 'Huamán', email: 'demo.empresa@sazloop.test', tieneFoto: false };
const EMPRESA = { id: 1, razonSocial: 'Sazón Criollo S.A.C.', nombreComercial: 'Sazón Criollo', ruc: '20123456789' };

const LOCALES = [
  { id: 1, nombre: 'Sazón Miraflores', distrito: 'Miraflores', direccion: 'Av. Larco 345', estado: 'ACTIVO', cuentaTrabajador: 'sazon.miraflores', latitud: -12.1211, longitud: -77.0297 },
  { id: 2, nombre: 'Sazón Barranco', distrito: 'Barranco', direccion: 'Jr. Unión 120', estado: 'ACTIVO', cuentaTrabajador: null, latitud: null, longitud: null },
  { id: 3, nombre: 'Sazón Surco', distrito: 'Santiago de Surco', direccion: 'Av. Primavera 900', estado: 'SUSPENDIDO', cuentaTrabajador: 'sazon.surco', latitud: null, longitud: null },
  { id: 4, nombre: 'Sazón Lince', distrito: 'Lince', direccion: 'Av. Arequipa 2000', estado: 'PENDIENTE_APROBACION', cuentaTrabajador: null, latitud: null, longitud: null },
  { id: 5, nombre: 'Sazón Callao', distrito: 'Callao', direccion: 'Av. Sáenz Peña 50', estado: 'PENDIENTE_APROBACION', cuentaTrabajador: null, latitud: null, longitud: null },
  { id: 6, nombre: 'Sazón Jesús María', distrito: 'Jesús María', direccion: 'Av. Brasil 700', estado: 'PENDIENTE_APROBACION', cuentaTrabajador: null, latitud: null, longitud: null }
];

const docAfiliacion = (id, tipo) => ({ id, tipo, numero: `N-${id}`, fechaEmision: '2026-01-10', fechaVencimiento: '2027-01-10' });
const solicitud = (establecimientoId, estado, documentos = [], extra = {}) => ({
  id: 100 + establecimientoId, establecimientoId, estado, documentos, motivoDecision: null, renovacion: false,
  fechaSolicitud: '2026-11-01T10:00:00', ...extra
});
const vigente = (id, tipo, vigencia, vencimiento = '2027-03-01') => ({ id, tipo, numero: `L-${id}`, vencimiento, vigencia });
const documentacion = (local, extra = {}) => ({
  establecimientoId: local.id, establecimiento: local.nombre, estado: local.estado, motivoSuspension: null,
  suspendidoDesde: null, documentos: [], tiposVencidos: [], tiposRenovables: [], solicitud: null, ...extra
});

const DOCUMENTACION = {
  1: documentacion(LOCALES[0], {
    documentos: [vigente(11, 'LICENCIA_FUNCIONAMIENTO', 'VIGENTE'), vigente(12, 'CERTIFICADO_SANITARIO', 'POR_VENCER', '2026-12-01'),
      vigente(13, 'OTRO_RARO', 'DESCONOCIDA', null)],
    tiposRenovables: ['CERTIFICADO_SANITARIO'],
    solicitud: solicitud(1, 'PENDIENTE', [docAfiliacion(21, 'CERTIFICADO_SANITARIO')], { renovacion: true })
  }),
  2: documentacion(LOCALES[1], {
    solicitud: solicitud(2, 'APROBADA', [], { renovacion: true, motivoDecision: 'documentos conformes' })
  }),
  3: documentacion(LOCALES[2], {
    motivoSuspension: 'Venció el certificado sanitario (venció el 09/11/2026)', suspendidoDesde: '2026-11-10T08:00:00',
    documentos: [vigente(31, 'CERTIFICADO_SANITARIO', 'VENCIDO', '2026-11-09')],
    tiposVencidos: ['CERTIFICADO_SANITARIO'], tiposRenovables: ['CERTIFICADO_SANITARIO', 'LICENCIA_FUNCIONAMIENTO'],
    solicitud: solicitud(3, 'RECHAZADA', [], { renovacion: true, motivoDecision: 'el archivo está borroso' })
  }),
  4: documentacion(LOCALES[3], {
    solicitud: solicitud(4, 'PENDIENTE', [docAfiliacion(41, 'LICENCIA_FUNCIONAMIENTO'), { id: 42, tipo: 'OTRO', numero: null, fechaEmision: null, fechaVencimiento: null }])
  }),
  5: documentacion(LOCALES[4], {
    solicitud: solicitud(5, 'RECHAZADA', [docAfiliacion(51, 'LICENCIA_FUNCIONAMIENTO')], { motivoDecision: 'la licencia no corresponde al local' })
  }),
  6: documentacion(LOCALES[5], { solicitud: solicitud(6, 'EN_REVISION', [docAfiliacion(61, 'INSPECCION_SEGURIDAD')]) })
};

const indicadores = (establecimientoId, establecimiento, extra = {}) => ({
  establecimientoId, establecimiento, ventas: 0, importeVentas: 0, comisiones: 0, ingresos: 0, cancelaciones: 0,
  noRecogidas: 0, cantidadResenas: 0, calificacionPromedio: 0, packsEntregados: 0, alimentoRecuperadoKg: 0,
  co2EvitadoKg: 0, resenasRecientes: [], ...extra
});
const INDICADORES = {
  establecimientos: [
    indicadores(1, 'Sazón Miraflores', {
      ventas: 12, importeVentas: 180, comisiones: 18, ingresos: 162, cancelaciones: 1, noRecogidas: 1, cantidadResenas: 2,
      calificacionPromedio: 4.5, packsEntregados: 10, alimentoRecuperadoKg: 5.25, co2EvitadoKg: 13.1,
      resenasRecientes: [{ calificacion: 5, comentario: 'Muy rico y abundante', fecha: '2026-11-20T19:30:00' },
        { calificacion: 4, comentario: '', fecha: '2026-11-18T13:00:00' }]
    }),
    indicadores(2, 'Sazón Barranco', { ventas: 3, importeVentas: 45, comisiones: 4.5, ingresos: 40.5, cantidadResenas: 1, calificacionPromedio: 3, packsEntregados: 3, alimentoRecuperadoKg: 1.5, co2EvitadoKg: 3.75 }),
    indicadores(99, 'Local dado de baja')
  ],
  total: indicadores(null, 'Total', {
    ventas: 15, importeVentas: 225, comisiones: 22.5, ingresos: 202.5, cancelaciones: 1, noRecogidas: 1, cantidadResenas: 3,
    calificacionPromedio: 4, packsEntregados: 13, alimentoRecuperadoKg: 6.75, co2EvitadoKg: null
  })
};

const AVISOS = [
  { id: 1, titulo: 'Solicitud aprobada', mensaje: 'Sazón Miraflores ya está activo', fecha: '2026-11-26T18:00:00', leida: false },
  { id: 2, titulo: 'Documento por vencer', mensaje: 'El certificado sanitario vence pronto', fecha: '2026-11-25T12:00:00', leida: false },
  { id: 3, titulo: 'Local suspendido', mensaje: 'Sazón Surco fue suspendido', fecha: '2026-11-10T08:00:00', leida: true }
];

let avisos = structuredClone(AVISOS);
let locales = structuredClone(LOCALES);
const servidor = {
  'GET /api/sesion': PROPIETARIO,
  'DELETE /api/sesion': respuesta(204),
  'GET /api/empresas/actual': EMPRESA,
  'GET /api/notificaciones': () => avisos,
  'GET /api/empresas/actual/indicadores': INDICADORES,
  'GET /api/empresas/actual/establecimientos': () => locales,
  'POST /api/notificaciones/1/leer': () => { avisos[0].leida = true; return respuesta(204); },
  'POST /api/notificaciones/leer-todas': () => { avisos.forEach(a => { a.leida = true; }); return respuesta(204); }
};
for (const id of Object.keys(DOCUMENTACION)) servidor[`GET /api/establecimientos/${id}/documentacion`] = () => DOCUMENTACION[id];

const pagina = abrirPagina('empresa.html', { servidor });
await import('../app/empresa.js');
await esperar();

const $ = pagina.$;
const textoDe = selector => $(selector).textContent.replace(/\s+/g, ' ').trim();
const vistaVisible = () => pagina.$$('[data-panel-vista]').filter(s => !s.hidden).map(s => s.dataset.panelVista);
const boton = (texto, raiz = pagina.document) => [...raiz.querySelectorAll('button')].find(b => b.textContent.trim() === texto);
const menu = vista => $(`.panel-lateral [data-vista="${vista}"]`);
const tituloAlertas = () => pagina.alertas.map(a => a.title);
const cifraDe = (raiz, etiqueta) => [...raiz.querySelectorAll('.cifra')].find(c => c.querySelector('span').textContent === etiqueta);

/** Abre el detalle de un local desde «Mis locales» → «Gestionar». */
async function abrirDetalle(nombre) {
  clic(menu('locales'));
  await esperar();
  const tarjeta = pagina.$$('#lista-locales .item-panel').find(t => t.querySelector('h3').textContent === nombre);
  clic(boton('Gestionar', tarjeta));
  await esperar();
  return $('[data-panel-vista="locales:detalle"]');
}

/** Envía un formulario creado por la página sin la validación del navegador (el archivo no se puede elegir en jsdom). */
function enviar(formulario) {
  formulario.dispatchEvent(new pagina.window.Event('submit', { bubbles: true, cancelable: true }));
}

/** Cambia la respuesta del GPS del navegador simulado. */
function gps(coordenadas) {
  Object.defineProperty(pagina.window.navigator, 'geolocation', {
    configurable: true,
    value: {
      getCurrentPosition(exito, fallo) {
        if (coordenadas) exito({ coords: coordenadas });
        else fallo({ code: 1, message: 'Permiso denegado' });
      }
    }
  });
}

test('carga la sesión: muestra el nombre del usuario, la empresa y el menú lateral', () => {
  assert.equal(textoDe('#nombre-usuario'), 'Rosa');
  assert.equal(textoDe('#titulo-panel'), 'Sazón Criollo');
  assert.equal(pagina.document.title, 'Sazloop | Sazón Criollo');
  assert.equal($('.panel-lateral').hidden, false);
  assert.deepEqual(vistaVisible(), ['resumen']);
  assert.equal(menu('resumen').getAttribute('aria-current'), 'page');
  assert.ok($('.panel-marca svg'), 'debe pintar el icono de la marca');
});

test('el contador de notificaciones muestra las no leídas', () => {
  assert.equal(pagina.llamadasA('GET', '/api/notificaciones').length, 1);
  assert.equal(textoDe('#contador-avisos'), '2');
});

test('el resumen muestra las cifras totales de la empresa con dinero y kilos de 1 decimal', () => {
  const cifras = $('#cifras-empresa');
  const valor = etiqueta => cifraDe(cifras, etiqueta).querySelector('strong').textContent;
  assert.equal(valor('Ventas'), '15');
  assert.match(cifraDe(cifras, 'Ventas').textContent, /S\/ 225\.00 vendidos/);
  assert.ok(cifraDe(cifras, 'Ventas').classList.contains('destacada'));
  assert.equal(valor('Ingresos de tus locales'), 'S/ 202.50');
  assert.equal(valor('Comisión de Sazloop'), 'S/ 22.50');
  assert.match(cifraDe(cifras, 'Comisión de Sazloop').textContent, /10 % de lo cobrado/);
  assert.equal(valor('Cancelaciones'), '1');
  assert.equal(valor('No recogidas'), '1');
  assert.equal(valor('Calificación'), '4.0 / 5');
  assert.match(cifraDe(cifras, 'Calificación').textContent, /3 reseñas/);
  assert.equal(valor('Packs entregados'), '13');
  assert.equal(valor('Comida recuperada'), '6,8 kg');
  assert.ok(cifraDe(cifras, 'Comida recuperada').classList.contains('impacto'));
  assert.equal(valor('CO₂ evitado'), '0,0 kg', 'un valor ausente cuenta como 0');
});

test('el resumen muestra las alertas de locales suspendidos, pendientes y sin cuenta', () => {
  const alertas = textoDe('#alertas-resumen');
  assert.match(alertas, /Sazón Surco está suspendido/);
  assert.match(alertas, /3 locales esperan aprobación/);
  assert.match(alertas, /Crea la cuenta del trabajador/);
  assert.match(alertas, /Sazón Barranco ya fue aprobado/);
  assert.equal($('#alertas-resumen .banda.peligro').getAttribute('role'), 'alert');
});

test('el detalle por establecimiento muestra las cifras de cada local y sus reseñas', () => {
  const tarjetas = pagina.$$('#detalle-establecimientos .tarjeta-panel');
  assert.equal(tarjetas.length, 3);
  const [miraflores, barranco, baja] = tarjetas;
  assert.match(miraflores.textContent, /Av\. Larco 345, Miraflores/);
  assert.equal(miraflores.querySelector('.insignia').textContent, 'Activo');
  assert.equal(cifraDe(miraflores, 'Ingresos del local').querySelector('strong').textContent, 'S/ 162.00');
  assert.equal(cifraDe(miraflores, 'Calificación').querySelector('strong').textContent, '4.5 / 5');
  assert.equal(cifraDe(miraflores, 'Comida recuperada').querySelector('strong').textContent, '5,3 kg');
  assert.equal(cifraDe(miraflores, 'CO₂ evitado').querySelector('strong').textContent, '13,1 kg');
  assert.match(miraflores.textContent, /★★★★★/);
  assert.match(miraflores.textContent, /★★★★☆/);
  assert.match(miraflores.textContent, /Muy rico y abundante/);
  assert.match(miraflores.textContent, /Sin comentario\./);
  assert.match(miraflores.textContent, /2026/);
  assert.match(cifraDe(barranco, 'Calificación').textContent, /1 reseña$/);
  assert.match(barranco.textContent, /Todavía no hay reseñas para este local\./);
  // Un local que ya no figura en la lista se muestra sin dirección ni estado.
  assert.equal(baja.querySelector('.insignia'), null);
  assert.equal(cifraDe(baja, 'Calificación').querySelector('strong').textContent, '—');
});

test('«Ver documentos» de la alerta de suspensión abre el detalle del local suspendido', async () => {
  pagina.alertas.length = 0;
  clic(boton('Ver documentos'));
  await esperar();
  const detalle = $('[data-panel-vista="locales:detalle"]');
  assert.deepEqual(vistaVisible(), ['locales:detalle']);
  assert.equal(menu('locales').getAttribute('aria-current'), 'page');
  const texto = detalle.textContent;
  assert.match(texto, /Sazón Surco/);
  assert.equal(detalle.querySelector('.vista-encabezado .insignia').textContent, 'Suspendido');
  assert.match(texto, /Local suspendido/);
  assert.match(texto, /Venció el certificado sanitario \(venció el 09\/11\/2026\)\. Suspendido desde el/);
  assert.match(texto, /Documentos del local/);
  assert.match(texto, /Vencido/);
  assert.match(texto, /La última renovación fue rechazada/);
  assert.match(texto, /Motivo: el archivo está borroso/);
  assert.match(texto, /Presenta solo los documentos vencidos: Certificado sanitario\./);
  assert.match(texto, /Adjuntar documento renovado/);
  assert.equal(detalle.querySelector('input[name="vencimiento"]').required, true);
  assert.equal(detalle.querySelectorAll('select[name="tipo"] option').length, 2);
  assert.match(texto, /Usuario del local: sazon\.surco/);
  assert.deepEqual(pagina.alertas, []);
});

test('«← Mis locales» vuelve a la lista de locales con su estado y lo que falta hacer', async () => {
  clic(boton('← Mis locales'));
  await esperar();
  assert.deepEqual(vistaVisible(), ['locales']);
  const tarjetas = pagina.$$('#lista-locales .item-panel');
  assert.equal(tarjetas.length, 6);
  const texto = nombre => tarjetas.find(t => t.querySelector('h3').textContent === nombre).textContent;
  assert.match(texto('Sazón Miraflores'), /Activo/);
  assert.match(texto('Sazón Miraflores'), /Cuenta del trabajador: sazon\.miraflores/);
  assert.match(texto('Sazón Barranco'), /Aún no tiene cuenta de trabajador\./);
  assert.match(texto('Sazón Surco'), /Suspendido/);
  assert.match(texto('Sazón Lince'), /Pendiente de aprobación/);
  assert.match(texto('Sazón Lince'), /Presenta sus documentos para que Sazloop lo apruebe\./);
  assert.match(texto('Sazón Lince'), /Av\. Arequipa 2000, Lince/);
  assert.equal($('#form-local').hidden, true);
});

test('«Agregar local» abre el formulario y «Cancelar» lo limpia y lo cierra', () => {
  clic($('#nuevo-local'));
  const formulario = $('#form-local');
  assert.equal(formulario.hidden, false);
  assert.equal(pagina.document.activeElement, formulario.elements.nombre);
  formulario.elements.nombre.value = 'Borrador';
  clic($('#cancelar-local'));
  assert.equal(formulario.hidden, true);
  assert.equal(formulario.elements.nombre.value, '');
  assert.equal(formulario.elements.provincia.value, 'Lima');
  assert.equal(textoDe('#estado-ubicacion'), 'Opcional: permite que los consumidores te encuentren con «Cerca de mí».');
});

test('«Usar mi ubicación actual» sin permiso explica cómo activarlo', async () => {
  clic($('#nuevo-local'));
  gps(null);
  clic($('#usar-ubicacion'));
  await esperar();
  assert.equal($('#estado-ubicacion').className, 'ubicacion-estado error');
  assert.match(textoDe('#estado-ubicacion'), /Activa el permiso de ubicación del navegador/);
  assert.equal($('#usar-ubicacion').disabled, false);
});

test('«Usar mi ubicación actual» con permiso captura las coordenadas con 6 decimales', async () => {
  gps({ latitude: -12.12345678, longitude: -77.03456789 });
  clic($('#usar-ubicacion'));
  await esperar();
  assert.equal($('#estado-ubicacion').className, 'ubicacion-estado lista');
  assert.equal(textoDe('#estado-ubicacion'), 'Ubicación capturada (-12.123457, -77.034568)');
});

test('si el servidor rechaza el nuevo local, se avisa en el formulario', async () => {
  servidor['POST /api/empresas/actual/establecimientos'] = respuesta(400, { message: 'Ingresa un distrito válido' });
  pagina.alertas.length = 0;
  llenarYEnviar($('#form-local'), { nombre: 'Sazón San Isidro', distrito: 'San Isidro', direccionLinea: 'Av. Conquistadores' });
  await esperar();
  const estado = $('#form-local .estado-formulario');
  assert.equal(estado.textContent, 'Ingresa un distrito válido');
  assert.equal(estado.className, 'estado-formulario error');
  assert.equal($('#form-local').hidden, false);
  assert.equal(pagina.ultimaAlerta().title, 'No se pudo registrar el local');
  assert.equal(pagina.ultimaAlerta().icon, 'warning');
});

test('registrar un local manda la dirección con la ubicación y abre su detalle', async () => {
  servidor['POST /api/empresas/actual/establecimientos'] = cuerpo => {
    const nuevo = { id: 7, nombre: cuerpo.nombre, distrito: cuerpo.direccion.distrito, direccion: `${cuerpo.direccion.direccionLinea} ${cuerpo.direccion.numero}`,
      estado: 'PENDIENTE_APROBACION', cuentaTrabajador: null, latitud: cuerpo.direccion.latitud, longitud: cuerpo.direccion.longitud };
    locales.push(nuevo);
    DOCUMENTACION[7] = documentacion(nuevo);
    servidor['GET /api/establecimientos/7/documentacion'] = () => DOCUMENTACION[7];
    return respuesta(201, nuevo);
  };
  pagina.alertas.length = 0;
  llenarYEnviar($('#form-local'), {
    nombre: 'Sazón San Isidro', telefono: '987654321', descripcion: 'Menú criollo', distrito: 'San Isidro',
    direccionLinea: 'Av. Conquistadores', numero: '455', referencia: 'Frente al parque', codigoPostal: '15073'
  });
  await esperar();
  const [llamada] = pagina.llamadasA('POST', '/api/empresas/actual/establecimientos').slice(-1);
  assert.deepEqual(llamada.cuerpo, {
    nombre: 'Sazón San Isidro', telefono: '987654321', descripcion: 'Menú criollo',
    direccion: { departamento: 'Lima', provincia: 'Lima', distrito: 'San Isidro', direccionLinea: 'Av. Conquistadores', numero: '455',
      referencia: 'Frente al parque', codigoPostal: '15073', latitud: -12.123457, longitud: -77.034568 }
  });
  assert.equal(llamada.opciones.headers['Content-Type'], 'application/json');
  assert.equal($('#form-local').hidden, true);
  assert.equal($('#form-local').elements.nombre.value, '');
  assert.deepEqual(vistaVisible(), ['locales:detalle']);
  const detalle = $('[data-panel-vista="locales:detalle"]');
  assert.match(detalle.textContent, /Sazón San Isidro/);
  assert.match(detalle.textContent, /Solicitud de afiliación/);
  assert.match(detalle.textContent, /1\. Adjunta los documentos del local/);
  assert.match(detalle.textContent, /Ubicación registrada/);
  assert.match(detalle.textContent, /Coordenadas: -12\.12346, -77\.03457\./);
  assert.match(detalle.textContent, /Podrás crear la cuenta del trabajador cuando Sazloop apruebe el local\./);
  assert.equal(detalle.querySelectorAll('select[name="tipo"] option').length, 4);
  assert.equal(boton('Enviar solicitud a revisión', detalle), undefined, 'sin documentos no se puede enviar');
  assert.ok(pagina.alertas.some(a => a.title === 'Local registrado' && a.icon === 'success'));
  assert.equal(textoDe('#mensaje'), 'Local registrado. Ahora adjunta sus documentos y envía la solicitud.');
});

test('adjuntar un documento lo sube como archivo y omite los campos vacíos', async () => {
  servidor['POST /api/establecimientos/7/documentos'] = () => {
    DOCUMENTACION[7].solicitud = solicitud(7, 'PENDIENTE', [docAfiliacion(71, 'CERTIFICADO_SANITARIO')]);
    return respuesta(201, docAfiliacion(71, 'CERTIFICADO_SANITARIO'));
  };
  pagina.alertas.length = 0;
  const formulario = $('[data-panel-vista="locales:detalle"] form');
  formulario.elements.tipo.value = 'CERTIFICADO_SANITARIO';
  formulario.elements.numero.value = 'CS-2026-001';
  formulario.elements.vencimiento.value = '2027-05-01';
  enviar(formulario);
  assert.equal(formulario.querySelector('button[type="submit"]').disabled, true, 'evita el doble envío');
  await esperar();
  const [llamada] = pagina.llamadasA('POST', '/api/establecimientos/7/documentos');
  assert.ok(llamada.cuerpo instanceof pagina.window.FormData);
  assert.equal(llamada.cuerpo.get('tipo'), 'CERTIFICADO_SANITARIO');
  assert.equal(llamada.cuerpo.get('numero'), 'CS-2026-001');
  assert.equal(llamada.cuerpo.get('vencimiento'), '2027-05-01');
  assert.equal(llamada.cuerpo.has('emision'), false);
  assert.equal(llamada.opciones.headers['Content-Type'], undefined, 'el navegador pone el límite del multipart');
  const detalle = $('[data-panel-vista="locales:detalle"]');
  assert.match(detalle.textContent, /N-71/);
  assert.ok(detalle.querySelector('a.enlace-archivo[href="/api/establecimientos/7/documentos/71/archivo"]'));
  assert.ok(boton('Enviar solicitud a revisión', detalle));
  assert.ok(pagina.alertas.some(a => a.title === 'Documento adjuntado'));
});

test('si el archivo no se acepta, se avisa y se puede volver a intentar', async () => {
  servidor['POST /api/establecimientos/7/documentos'] = respuesta(413);
  pagina.alertas.length = 0;
  const formulario = $('[data-panel-vista="locales:detalle"] form');
  enviar(formulario);
  await esperar();
  assert.equal(formulario.querySelector('.estado-formulario').textContent, 'El archivo supera el tamaño permitido.');
  assert.equal(formulario.querySelector('button[type="submit"]').disabled, false);
  assert.equal(pagina.ultimaAlerta().title, 'No se pudo adjuntar el documento');
});

test('si falla el envío de la solicitud, se avisa y el botón se reactiva', async () => {
  servidor['POST /api/establecimientos/7/solicitud/enviar'] = respuesta(409, { message: 'Falta la licencia de funcionamiento' });
  pagina.alertas.length = 0;
  const detalle = $('[data-panel-vista="locales:detalle"]');
  const enviarBoton = boton('Enviar solicitud a revisión', detalle);
  clic(enviarBoton);
  await esperar();
  assert.equal(enviarBoton.disabled, false);
  assert.equal(enviarBoton.parentElement.nextElementSibling.textContent, 'Falta la licencia de funcionamiento');
  assert.equal(pagina.ultimaAlerta().title, 'No se pudo enviar la solicitud');
});

test('enviar la solicitud la deja en revisión', async () => {
  servidor['POST /api/establecimientos/7/solicitud/enviar'] = () => {
    DOCUMENTACION[7].solicitud.estado = 'EN_REVISION';
    return respuesta(204);
  };
  pagina.alertas.length = 0;
  const detalle = $('[data-panel-vista="locales:detalle"]');
  clic(boton('Enviar solicitud a revisión', detalle));
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/establecimientos/7/solicitud/enviar').length, 2);
  assert.match(detalle.textContent, /Solicitud en revisión/);
  assert.equal(detalle.querySelector('form'), null, 'en revisión ya no se adjuntan documentos');
  assert.ok(pagina.alertas.some(a => a.title === 'Enviado a revisión'));
});

test('una solicitud pendiente con documentos los lista y permite enviarla', async () => {
  const detalle = await abrirDetalle('Sazón Lince');
  assert.match(detalle.textContent, /Solicitud de afiliación/);
  const filas = [...detalle.querySelectorAll('tbody tr')].map(f => f.textContent);
  assert.match(filas[0], /Licencia de funcionamiento/);
  assert.match(filas[1], /Otro documento—/);
  assert.ok(boton('Enviar solicitud a revisión', detalle));
  assert.ok(detalle.querySelector('form'));
});

test('una solicitud rechazada muestra el motivo y permite adjuntar nuevos documentos', async () => {
  const detalle = await abrirDetalle('Sazón Callao');
  assert.match(detalle.textContent, /Solicitud rechazada/);
  assert.match(detalle.textContent, /Motivo: la licencia no corresponde al local\./);
  assert.equal(detalle.querySelector('table'), null, 'los documentos rechazados no se listan');
  assert.ok(detalle.querySelector('form'));
  assert.equal(boton('Enviar solicitud a revisión', detalle), undefined);
});

test('una solicitud en revisión muestra sus documentos sin formulario', async () => {
  const detalle = await abrirDetalle('Sazón Jesús María');
  assert.match(detalle.textContent, /Solicitud en revisión/);
  assert.match(detalle.textContent, /Inspección de seguridad/);
  assert.equal(detalle.querySelector('form'), null);
  assert.match(detalle.textContent, /Sin ubicación/);
});

test('un local activo muestra sus documentos vigentes, la renovación pendiente y su cuenta', async () => {
  const detalle = await abrirDetalle('Sazón Miraflores');
  const texto = detalle.textContent;
  assert.match(texto, /Documentos del local/);
  assert.match(texto, /Vigente/);
  assert.match(texto, /Por vencer/);
  assert.match(texto, /OTRO_RARO/, 'un tipo desconocido se muestra tal cual');
  assert.match(texto, /DESCONOCIDA/);
  assert.match(texto, /Vencen en los próximos días: Certificado sanitario\. Renuévalos antes del vencimiento/);
  assert.ok(boton('Enviar renovación a revisión', detalle));
  assert.match(texto, /Usuario del local: sazon\.miraflores/);
  assert.match(texto, /Ubicación registrada/);
  assert.match(texto, /Coordenadas: -12\.12110, -77\.02970\./);
});

test('un local activo sin renovaciones pendientes lo indica', async () => {
  const detalle = await abrirDetalle('Sazón Barranco');
  assert.match(detalle.textContent, /No hay documentos aprobados registrados\./);
  assert.match(detalle.textContent, /Última renovación aprobada: documentos conformes/);
  assert.match(detalle.textContent, /No hay documentos por renovar\. Te avisaremos siete días antes/);
  assert.match(detalle.textContent, /Sin ubicación/);
  assert.match(detalle.textContent, /Sin coordenadas, este local aparece al final/);
});

test('«Actualizar ubicación» sin permiso avisa y vuelve a habilitar el botón', async () => {
  gps(null);
  pagina.alertas.length = 0;
  const actualizar = boton('Actualizar ubicación', $('[data-panel-vista="locales:detalle"]'));
  clic(actualizar);
  await esperar();
  assert.equal(actualizar.disabled, false);
  assert.equal(actualizar.textContent, 'Actualizar ubicación');
  assert.equal(pagina.ultimaAlerta().title, 'No se pudo actualizar la ubicación');
  assert.match(pagina.ultimaAlerta().text, /Activa el permiso de ubicación/);
});

test('«Actualizar ubicación» con permiso guarda las coordenadas del local', async () => {
  servidor['PUT /api/empresas/actual/establecimientos/2/ubicacion'] = cuerpo => {
    Object.assign(locales[1], cuerpo.latitud ? { latitud: cuerpo.latitud, longitud: cuerpo.longitud } : {});
    return respuesta(204);
  };
  gps({ latitude: -12.1500001, longitude: -77.0200009 });
  pagina.alertas.length = 0;
  const actualizar = boton('Actualizar ubicación', $('[data-panel-vista="locales:detalle"]'));
  clic(actualizar);
  assert.equal(actualizar.disabled, true);
  await esperar();
  assert.deepEqual(pagina.llamadasA('PUT', '/api/empresas/actual/establecimientos/2/ubicacion')[0].cuerpo, { latitud: -12.15, longitud: -77.020001 });
  const detalle = $('[data-panel-vista="locales:detalle"]');
  assert.match(detalle.textContent, /Ubicación registrada/);
  assert.ok(pagina.alertas.some(a => a.title === 'Ubicación actualizada'));
});

test('crear la cuenta del trabajador con un usuario repetido muestra el error', async () => {
  servidor['POST /api/establecimientos/2/cuenta'] = respuesta(409, { message: 'Ese usuario ya existe' });
  pagina.alertas.length = 0;
  const formulario = $('[data-panel-vista="locales:detalle"] input[name="nombreUsuario"]').form;
  llenarYEnviar(formulario, { nombreUsuario: 'sazon.miraflores', contrasena: 'Clave12345' });
  await esperar();
  assert.equal(formulario.querySelector('.estado-formulario').textContent, 'Ese usuario ya existe');
  assert.equal(pagina.ultimaAlerta().title, 'No se pudo crear la cuenta');
});

test('crear la cuenta del trabajador la registra y la muestra en el local', async () => {
  servidor['POST /api/establecimientos/2/cuenta'] = cuerpo => { locales[1].cuentaTrabajador = cuerpo.nombreUsuario; return respuesta(201, { id: 9 }); };
  pagina.alertas.length = 0;
  const formulario = $('[data-panel-vista="locales:detalle"] input[name="nombreUsuario"]').form;
  llenarYEnviar(formulario, { nombreUsuario: 'sazon.barranco', contrasena: 'Clave12345' });
  await esperar();
  assert.deepEqual(pagina.llamadasA('POST', '/api/establecimientos/2/cuenta').at(-1).cuerpo,
    { nombreUsuario: 'sazon.barranco', contrasena: 'Clave12345' });
  const detalle = $('[data-panel-vista="locales:detalle"]');
  assert.match(detalle.textContent, /Usuario del local: sazon\.barranco/);
  assert.equal(detalle.querySelector('input[name="nombreUsuario"]'), null);
  assert.ok(pagina.alertas.some(a => a.title === 'Cuenta creada'));
});

test('al volver a la pestaña con un formulario abierto en el detalle solo se actualiza el contador', async () => {
  await abrirDetalle('Sazón Lince');
  const antesDoc = pagina.llamadasA('GET', '/api/establecimientos/4/documentacion').length;
  const antesAvisos = pagina.llamadasA('GET', '/api/notificaciones').length;
  avisos[1].leida = true;
  pagina.document.activeElement?.blur(); // el campo «Nombre del local» quedó enfocado al abrir el formulario
  pagina.document.dispatchEvent(new pagina.window.Event('visibilitychange'));
  await esperar();
  assert.equal(pagina.llamadasA('GET', '/api/notificaciones').length, antesAvisos + 1);
  assert.equal(pagina.llamadasA('GET', '/api/establecimientos/4/documentacion').length, antesDoc, 'no se pierde lo escrito');
  assert.equal(textoDe('#contador-avisos'), '1');
});

test('al volver a la pestaña mientras se escribe en un campo no se recarga nada', async () => {
  const campo = $('[data-panel-vista="locales:detalle"] input[name="numero"]');
  campo.focus();
  const antes = pagina.llamadas.length;
  pagina.document.dispatchEvent(new pagina.window.Event('visibilitychange'));
  await esperar();
  assert.equal(pagina.llamadas.length, antes);
  campo.blur();
});

test('al volver a la pestaña en el resumen se recargan el contador y la vista', async () => {
  avisos[1].leida = false;
  clic(menu('resumen'));
  await esperar();
  const antes = pagina.llamadasA('GET', '/api/empresas/actual/indicadores').length;
  pagina.document.dispatchEvent(new pagina.window.Event('visibilitychange'));
  await esperar();
  assert.equal(pagina.llamadasA('GET', '/api/empresas/actual/indicadores').length, antes + 1);
  assert.equal(textoDe('#contador-avisos'), '2');
  assert.deepEqual(vistaVisible(), ['resumen']);
});

test('si fallan los indicadores, el resumen lo avisa y mantiene las alertas', async () => {
  servidor['GET /api/empresas/actual/indicadores'] = respuesta(500, { message: 'NullPointerException' });
  const consola = console.error;
  console.error = () => {};
  try {
    clic($('#actualizar-resumen'));
    await esperar();
  } finally { console.error = consola; }
  assert.match(textoDe('#cifras-empresa'), /No se pudieron cargar los indicadores/);
  assert.match(textoDe('#cifras-empresa'), /código 500/);
  assert.equal($('#detalle-establecimientos').children.length, 0);
  assert.match(textoDe('#alertas-resumen'), /Sazón Surco está suspendido/);
});

test('si se pierde la conexión al actualizar el resumen, se avisa «Sin conexión»', async () => {
  servidor['GET /api/empresas/actual/indicadores'] = INDICADORES;
  servidor['GET /api/empresas/actual/establecimientos'] = new TypeError('Failed to fetch');
  clic($('#actualizar-resumen'));
  await esperar();
  // Sin la lista de locales, el resumen se muestra sin alertas ni datos de dirección.
  assert.equal(textoDe('#alertas-resumen'), '');
  assert.equal(pagina.$$('#detalle-establecimientos .tarjeta-panel').length, 3);
  assert.equal(pagina.$('#detalle-establecimientos .vista-encabezado .nota'), null);
});

test('si no se pueden cargar los locales, se avisa sin conexión', async () => {
  pagina.alertas.length = 0;
  clic(menu('locales'));
  await esperar();
  assert.equal(textoDe('#mensaje'), 'Sin conexión. Revisa tu internet e intenta de nuevo.');
  assert.equal($('#mensaje').className, 'error');
  assert.equal(pagina.ultimaAlerta().title, 'Sin conexión');
  servidor['GET /api/empresas/actual/establecimientos'] = () => locales;
});

test('si «Actualizar» del resumen falla por completo, se avisa con una alerta', async () => {
  // Una respuesta sin forma de indicadores hace fallar el pintado del resumen.
  servidor['GET /api/empresas/actual/indicadores'] = { establecimientos: null, total: INDICADORES.total };
  pagina.alertas.length = 0;
  clic($('#actualizar-resumen'));
  await esperar();
  assert.equal(pagina.ultimaAlerta().title, 'No se pudo actualizar el resumen');
  servidor['GET /api/empresas/actual/indicadores'] = INDICADORES;
});

test('Notificaciones lista los avisos y marca uno como leído', async () => {
  avisos = structuredClone(AVISOS);
  clic(menu('avisos'));
  await esperar();
  assert.deepEqual(vistaVisible(), ['avisos']);
  assert.equal(pagina.$$('#lista-avisos .aviso-item').length, 3);
  assert.equal(pagina.$$('#lista-avisos .aviso-item.no-leida').length, 2);
  assert.match(textoDe('#lista-avisos'), /Solicitud aprobada/);
  clic(boton('Marcar como leída'));
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/notificaciones/1/leer').length, 1);
  assert.equal(pagina.$$('#lista-avisos .aviso-item.no-leida').length, 1);
  assert.equal(textoDe('#contador-avisos'), '1');
});

test('«Marcar todas como leídas» deja el contador vacío', async () => {
  clic(boton('Marcar todas como leídas'));
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/notificaciones/leer-todas').length, 1);
  assert.equal(pagina.$$('#lista-avisos .aviso-item.no-leida').length, 0);
  assert.equal(boton('Marcar todas como leídas'), undefined);
  assert.equal(textoDe('#contador-avisos'), '');
});

test('sin notificaciones muestra el estado vacío', async () => {
  avisos = [];
  clic(menu('avisos'));
  await esperar();
  assert.match(textoDe('#lista-avisos'), /No tienes notificaciones/);
});

test('si las notificaciones fallan al volver a la pestaña, el contador no rompe la página', async () => {
  servidor['GET /api/notificaciones'] = respuesta(401);
  pagina.alertas.length = 0;
  clic(menu('resumen'));
  await esperar();
  pagina.document.dispatchEvent(new pagina.window.Event('visibilitychange'));
  await esperar();
  assert.equal(textoDe('#contador-avisos'), '');
  assert.deepEqual(tituloAlertas(), []);
});

test('«Cerrar sesión» cierra la sesión aunque ya no existiera y vuelve al acceso de locales', async () => {
  servidor['DELETE /api/sesion'] = respuesta(401);
  clic($('#salir'));
  await esperar();
  assert.equal(pagina.llamadasA('DELETE', '/api/sesion').length, 1);
  assert.deepEqual(pagina.navegaciones, ['acceso-locales.html']);
  servidor['DELETE /api/sesion'] = respuesta(204);
  clic($('#salir'));
  await esperar();
  assert.equal(pagina.llamadasA('DELETE', '/api/sesion').length, 2);
  assert.equal(pagina.navegaciones.length, 2);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
