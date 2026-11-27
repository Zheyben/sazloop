// Portal del trabajador del local (app/trabajador.html + trabajador.js).
// Una sola carga de la página: empieza sin pack publicado, publica uno, valida códigos y cierra sesión.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, respuesta } from './navegador.js';

const periodo = (ganancia, packs) => ({ ganancia, unidadesNoVendidas: 2, ventaBruta: ganancia / 0.9, comision: ganancia / 9, packsEntregados: packs });

const OFERTA = {
  id: 41, establecimientoId: 2, establecimiento: 'Aurora Miraflores', distrito: 'Miraflores', direccion: 'Av. Larco 345',
  pack: 'Pack Aurora clásico', descripcion: 'Panes y postres del día', posiblesProductos: 'Croissants, empanadas y alfajores',
  alergenos: ['Trigo (gluten)', 'Leche'], restricciones: ['Apto para vegetarianos', 'No apto para veganos', 'No apto para personas con celiaquía'],
  inicioRecojo: '2026-11-27T18:00:00', finRecojo: '2026-11-27T21:00:00', estado: 'ACTIVA',
  variantes: [
    { id: 1, tamanio: 'PEQUENO', precioReferencial: 20, precioDescuento: 8, ahorro: 12, stockDisponible: 3 },
    { id: 2, tamanio: 'GRANDE', precioReferencial: 40, precioDescuento: 15, ahorro: 25, stockDisponible: 1 }
  ]
};

function resumen({ oferta = null, estadoLocal = 'ACTIVO', motivoSuspension = null, tokenQr = 'tok-aurora' } = {}) {
  return {
    establecimiento: 'Aurora Miraflores', ofertaVigente: oferta, tokenQr,
    stockDisponible: oferta ? 4 : 0, dia: periodo(18, 1), semana: periodo(90, 5), mes: periodo(360, 20),
    estadoLocal, motivoSuspension
  };
}

const COMPRA = {
  reservaId: 9, establecimientoId: 2, establecimiento: 'Aurora Miraflores', estado: 'CONFIRMADA', total: 23,
  medioPago: 'YAPE', codigoRecojo: '805949F6-20C9-42', inicioRecojo: '2026-11-27T18:00:00', finRecojo: '2026-11-27T21:00:00',
  cliente: 'Valeria Quispe', modalidad: 'RECOJO',
  lineas: [{ tamanio: 'PEQUENO', cantidad: 1, precioUnitario: 8 }, { tamanio: 'GRANDE', cantidad: 1, precioUnitario: 15 }]
};

let estadoResumen = resumen();
const pagina = abrirPagina('trabajador.html', {
  servidor: {
    'GET /api/local/sesion': { establecimientoId: 2, establecimiento: 'Aurora Miraflores' },
    'GET /api/local/resumen': () => estadoResumen,
    'GET /api/local/packs': [{ id: 3, nombre: 'Pack Aurora clásico' }],
    'DELETE /api/local/sesion': respuesta(204)
  }
});
await import('../app/trabajador.js');
await esperar();

const form = pagina.$('#form-pack');
const titulos = () => pagina.alertas.map(a => a.title);
const boton = texto => pagina.$$('button').find(b => b.textContent.trim() === texto);
const menu = vista => pagina.$(`.panel-lateral [data-vista="${vista}"]`);
function escribir(nombre, valor) {
  const campo = form.elements[nombre];
  if (campo.type === 'checkbox') campo.checked = Boolean(valor); else campo.value = valor;
  campo.dispatchEvent(new pagina.window.Event('input', { bubbles: true }));
  campo.dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
}
const pasoVisible = () => pagina.$$('.paso-formulario').findIndex(p => !p.hidden);
const estadoPack = () => pagina.$('#estado-pack').textContent;

// ---------- Carga inicial sin pack publicado ----------

test('carga el local y su resumen sin pack publicado', () => {
  assert.equal(pagina.$('#titulo-panel').textContent, 'Aurora Miraflores');
  assert.equal(pagina.document.title, 'Sazloop | Aurora Miraflores');
  assert.match(pagina.texto(), /No hay un pack sorpresa vigente/);
  assert.match(pagina.$('#cifras-stock').textContent, /0 unidades/);
  assert.match(pagina.$('#cifras-stock').textContent, /Sin pack publicado/);
  assert.match(pagina.$('#cifras-stock').textContent, /1 pack entregado/);
  assert.equal(pagina.$$('#periodos .periodo').length, 3);
  assert.match(pagina.$('#periodos').textContent, /Esta semana.*Comisión de Sazloop/);
  assert.equal(pagina.$('#aviso-suspension').children.length, 0);
  // Hay un pack registrado: se puede reutilizar.
  assert.equal(form.querySelector('[name="origen"][value="existente"]').disabled, false);
  assert.equal(form.elements.packId.options[0].textContent, 'Pack Aurora clásico');
});

test('el botón «Crear nuevo pack» del aviso vacío abre el formulario sin bloqueos', () => {
  clic(pagina.$('#pack-vigente button'));
  assert.equal(pagina.$('[data-panel-vista="crear"]').hidden, false);
  assert.equal(menu('crear').getAttribute('aria-current'), 'page');
  assert.equal(pagina.$('#aviso-publicacion').children.length, 0);
  assert.equal(pagina.$('#paso-siguiente').disabled, false);
  assert.equal(pagina.$$('#alergenos input').length, 8);
  assert.equal(pagina.$$('#variantes fieldset').length, 3);
  assert.ok(form.elements.inicioRecojo.value && form.elements.finRecojo.value, 'debe sugerir un horario');
});

// ---------- Crear nuevo pack ----------

test('paso 1: no avanza si falta el nombre del pack', () => {
  clic(pagina.$('#paso-siguiente'));
  assert.equal(pasoVisible(), 0);
});

test('paso 1 completo avanza a «Tamaños y precios» y «Atrás» regresa', () => {
  escribir('nombre', 'Pack panadería del día');
  escribir('descripcion', 'Excedentes de panadería');
  escribir('posiblesProductos', 'Pan de molde, croissants');
  pagina.$('#alergenos input[value="Huevo"]').checked = true;
  escribir('otroAlergeno', 'Sésamo');
  escribir('aptoVegetarianos', true);
  clic(pagina.$('#paso-siguiente'));
  assert.equal(pasoVisible(), 1);
  assert.equal(pagina.$('#paso-anterior').hidden, false);
  assert.equal(pagina.$('.pasos li[data-paso="0"]').classList.contains('completo'), true);
  clic(pagina.$('#paso-anterior'));
  assert.equal(pasoVisible(), 0);
  assert.equal(pagina.$('#paso-anterior').hidden, true);
  clic(pagina.$('#paso-siguiente'));
  assert.equal(pasoVisible(), 1);
});

test('paso 2: exige precios y que el descuento sea menor que el precio regular', () => {
  clic(pagina.$('#paso-siguiente'));
  assert.equal(pasoVisible(), 1, 'sin precios no avanza');
  escribir('precioReferencial-PEQUENO', '20');
  escribir('precioDescuento-PEQUENO', '25');
  escribir('stock-PEQUENO', '5');
  pagina.alertas.length = 0;
  clic(pagina.$('#paso-siguiente'));
  assert.equal(pasoVisible(), 1);
  assert.equal(estadoPack(), 'El precio con descuento debe ser menor que el precio regular.');
  assert.ok(titulos().includes('Revisa los precios'));
  escribir('precioDescuento-PEQUENO', '8');
  assert.match(pagina.$('#variantes .descuento').textContent, /60 % de descuento/);
});

test('paso 2: pide incluir al menos un tamaño', () => {
  escribir('incluir-PEQUENO', false);
  assert.equal(form.elements['precioReferencial-PEQUENO'].disabled, true);
  pagina.alertas.length = 0;
  clic(pagina.$('#paso-siguiente'));
  assert.equal(estadoPack(), 'Incluye al menos un tamaño.');
  assert.ok(titulos().includes('Falta un tamaño'));
  escribir('incluir-PEQUENO', true);
  escribir('incluir-GRANDE', true);
  escribir('precioReferencial-GRANDE', '40');
  escribir('precioDescuento-GRANDE', '15');
  escribir('stock-GRANDE', '2');
  clic(pagina.$('#paso-siguiente'));
  assert.equal(pasoVisible(), 2);
  assert.equal(pagina.$('#publicar').hidden, false);
  assert.equal(pagina.$('#paso-siguiente').hidden, true);
});

test('paso 3: muestra la revisión del pack antes de publicar', () => {
  const revision = pagina.$('#resumen-publicacion').textContent;
  assert.match(revision, /Así se publicará/);
  assert.match(revision, /Pack panadería del día/);
  assert.match(revision, /Pequeño.*S\/ 20\.00.*S\/ 8\.00.*5/);
  assert.match(revision, /Grande.*S\/ 40\.00.*S\/ 15\.00.*2/);
  assert.match(revision, /Recojo:/);
});

test('paso 3: rechaza un horario en el pasado o sin fin', async () => {
  const fin = form.elements.finRecojo.value;
  form.elements.inicioRecojo.value = '2020-01-01T10:00';
  form.elements.inicioRecojo.dispatchEvent(new pagina.window.Event('input'));
  pagina.alertas.length = 0;
  form.requestSubmit();
  await esperar();
  assert.equal(estadoPack(), 'El inicio del recojo no puede estar en el pasado.');
  assert.equal(pagina.llamadasA('POST', '/api/local/publicaciones').length, 0);

  const inicio = new Date(Date.now() + 3600000);
  const local = d => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  form.elements.inicioRecojo.value = local(inicio);
  form.elements.finRecojo.value = local(new Date(inicio.getTime() - 3600000));
  form.elements.finRecojo.dispatchEvent(new pagina.window.Event('input'));
  assert.match(pagina.$('#resumen-publicacion').textContent, /Indica el horario de recojo/);
  form.requestSubmit();
  await esperar();
  assert.equal(estadoPack(), 'El fin del recojo debe ser posterior al inicio.');
  assert.ok(titulos().includes('Revisa el horario'));
  form.elements.finRecojo.value = fin;
  form.elements.inicioRecojo.value = local(new Date());
});

test('publicar un pack nuevo envía el pack completo; si ya hay oferta activa lo explica', async () => {
  pagina.servidor['POST /api/local/publicaciones'] = respuesta(409, { message: 'El local ya tiene una oferta activa' });
  pagina.alertas.length = 0;
  form.requestSubmit();
  await esperar();
  const [llamada] = pagina.llamadasA('POST', '/api/local/publicaciones');
  assert.deepEqual(llamada.cuerpo.pack, {
    nombre: 'Pack panadería del día',
    descripcion: 'Excedentes de panadería',
    posiblesProductos: 'Pan de molde, croissants',
    alergenos: ['Huevo', 'Sésamo'],
    restricciones: ['Apto para vegetarianos', 'No apto para veganos', 'No apto para personas con celiaquía']
  });
  assert.deepEqual(llamada.cuerpo.variantes, [
    { tamanio: 'PEQUENO', precioReferencial: '20', precioDescuento: '8', stock: 5 },
    { tamanio: 'GRANDE', precioReferencial: '40', precioDescuento: '15', stock: 2 }
  ]);
  assert.ok(llamada.cuerpo.inicioRecojo && llamada.cuerpo.finRecojo);
  assert.equal(estadoPack(), 'Este local ya tiene una oferta activa. Podrás publicar otra cuando termine su horario.');
  assert.ok(titulos().includes('Ya hay una oferta activa'));
  assert.equal(pagina.$('#publicar').disabled, false);
});

test('otro error al publicar muestra el mensaje del servidor', async () => {
  pagina.servidor['POST /api/local/publicaciones'] = respuesta(400, { message: 'El stock debe ser mayor que cero' });
  pagina.alertas.length = 0;
  form.requestSubmit();
  await esperar();
  assert.equal(estadoPack(), 'El stock debe ser mayor que cero');
  assert.ok(titulos().includes('No se pudo publicar el pack'));
});

test('publicar un pack ya registrado lo publica y vuelve a «Pack sorpresa»', async () => {
  const existente = form.querySelector('[name="origen"][value="existente"]');
  existente.checked = true;
  existente.dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
  assert.equal(pagina.$('#pack-existente').hidden, false);
  assert.equal(pagina.$('#pack-nuevo').hidden, true);
  assert.equal(form.elements.nombre.disabled, true);
  // La revisión se vuelve a pintar al cambiar el horario y ahora muestra el pack registrado.
  form.elements.finRecojo.dispatchEvent(new pagina.window.Event('input'));
  assert.match(pagina.$('#resumen-publicacion').textContent, /Pack Aurora clásico/);

  pagina.servidor['POST /api/local/ofertas'] = cuerpo => { estadoResumen = resumen({ oferta: OFERTA }); return { id: 41, ...cuerpo }; };
  pagina.alertas.length = 0;
  form.requestSubmit();
  await esperar();
  const [llamada] = pagina.llamadasA('POST', '/api/local/ofertas');
  assert.equal(llamada.cuerpo.packId, 3);
  assert.equal(llamada.cuerpo.pack, undefined);
  assert.equal(pagina.$('[data-panel-vista="vigente"]').hidden, false);
  assert.ok(titulos().includes('Pack publicado'));
  assert.match(pagina.$('#mensaje').textContent, /Pack publicado/);
  // El formulario queda limpio y en el primer paso.
  assert.equal(pasoVisible(), 0);
  assert.equal(form.elements.nombre.value, '');
  assert.equal(form.elements.origen.value, 'nuevo');
});

// ---------- Pack publicado ----------

test('muestra el pack publicado, visible para los consumidores, con su QR', () => {
  const zona = pagina.$('#pack-vigente').textContent;
  assert.match(zona, /Pack publicado/);
  assert.match(zona, /Pack Aurora clásico/);
  assert.match(zona, /Panes y postres del día/);
  assert.match(zona, /Podría incluir.*Croissants, empanadas y alfajores/);
  assert.match(zona, /Visible para los consumidores/);
  assert.match(zona, /Horario de recojo/);
  assert.match(zona, /Trigo \(gluten\).*Leche/);
  assert.match(zona, /Apto para vegetarianos/);
  assert.match(zona, /Pequeño.*S\/ 20\.00.*S\/ 8\.00.*3/);
  assert.match(pagina.$('#cifras-stock').textContent, /4 unidades/);
  assert.match(pagina.$('#cifras-stock').textContent, /Pequeño: 3 · Grande: 1/);
  const img = pagina.$('.qr-oferta img');
  assert.ok(img.src.startsWith('data:image/'), 'el QR debe ser una imagen');
  const enlace = new URL(pagina.$('.qr-oferta a').href);
  assert.equal(enlace.pathname, '/app/qr.html');
  assert.equal(enlace.searchParams.get('oferta'), '41');
  assert.equal(enlace.searchParams.get('token'), 'tok-aurora');
});

test('con un pack vigente el formulario de publicación queda bloqueado', () => {
  clic(menu('crear'));
  assert.match(pagina.$('#aviso-publicacion').textContent, /Ya tienes un pack publicado/);
  assert.ok(pagina.$$('fieldset.paso-formulario').every(f => f.disabled));
  assert.equal(pagina.$('#paso-siguiente').disabled, true);
});

test('volver a «Pack sorpresa» recarga el resumen del local', async () => {
  const antes = pagina.llamadasA('GET', '/api/local/resumen').length;
  clic(menu('vigente'));
  await esperar();
  assert.equal(pagina.llamadasA('GET', '/api/local/resumen').length, antes + 1);
  assert.equal(pagina.$('[data-panel-vista="vigente"]').hidden, false);
});

test('pack sin alérgenos declarados lo indica', async () => {
  estadoResumen = resumen({ oferta: { ...OFERTA, alergenos: [] } });
  clic(menu('vigente'));
  await esperar();
  assert.match(pagina.$('#pack-vigente').textContent, /Sin alérgenos declarados/);
});

test('si el QR no se puede generar, igual muestra la información del pack', async () => {
  estadoResumen = resumen({ oferta: OFERTA, tokenQr: 'x'.repeat(5000) });
  const errorOriginal = console.error;
  const registrados = [];
  console.error = (...args) => registrados.push(args);
  try {
    clic(menu('vigente'));
    await esperar();
  } finally { console.error = errorOriginal; }
  assert.match(pagina.$('#pack-vigente').textContent, /No se pudo generar el código QR de esta oferta/);
  assert.match(pagina.$('#pack-vigente').textContent, /Pack Aurora clásico/);
  assert.equal(registrados[0][0], '[Sazloop] No se pudo generar el QR');
});

// ---------- Local suspendido ----------

test('local suspendido: banda de aviso y pack oculto a los consumidores', async () => {
  estadoResumen = resumen({ oferta: OFERTA, estadoLocal: 'SUSPENDIDO', motivoSuspension: 'Certificado sanitario vencido (venció el 09/11/2026)' });
  clic(menu('vigente'));
  await esperar();
  const aviso = pagina.$('#aviso-suspension').textContent;
  assert.match(aviso, /Local suspendido por documentos vencidos/);
  assert.match(aviso, /Certificado sanitario vencido \(venció el 09\/11\/2026\)\. Puedes validar códigos/);
  assert.match(pagina.$('#pack-vigente').textContent, /Oculto mientras el local esté suspendido/);
  assert.doesNotMatch(pagina.$('#pack-vigente').textContent, /Visible para los consumidores/);
});

test('local suspendido: no permite publicar', () => {
  clic(menu('crear'));
  assert.match(pagina.$('#aviso-publicacion').textContent, /No puedes publicar mientras el local esté suspendido/);
  assert.match(pagina.$('#aviso-publicacion').textContent, /renovar los documentos vencidos/);
  assert.ok(pagina.$$('fieldset.paso-formulario').every(f => f.disabled));
  assert.equal(pagina.$('#publicar').disabled, true);
});

test('local suspendido sin pack: no ofrece crear uno y usa el motivo por defecto', async () => {
  estadoResumen = resumen({ estadoLocal: 'SUSPENDIDO' });
  clic(menu('vigente'));
  await esperar();
  assert.match(pagina.$('#aviso-suspension').textContent, /Un documento obligatorio venció\./);
  assert.match(pagina.$('#pack-vigente').textContent, /El local está suspendido; no puede publicar packs por ahora/);
  assert.equal(pagina.$('#pack-vigente button'), null);
});

test('si falla la carga del local, avisa con un mensaje', async () => {
  pagina.servidor['GET /api/local/resumen'] = respuesta(500, { message: 'Error' });
  pagina.alertas.length = 0;
  const errorOriginal = console.error;
  console.error = () => {};
  try {
    clic(menu('vigente'));
    await esperar();
  } finally { console.error = errorOriginal; }
  assert.ok(titulos().includes('No se pudo cargar el local'));
  assert.match(pagina.$('#mensaje').textContent, /error inesperado en el servidor \(código 500\)/);
  estadoResumen = resumen({ oferta: OFERTA });
  pagina.servidor['GET /api/local/resumen'] = () => estadoResumen;
});

// ---------- Validar código ----------

const formCodigo = pagina.$('#form-codigo');
const resultado = () => pagina.$('#resultado-codigo').textContent;
async function consultar(codigo) {
  formCodigo.elements.codigo.value = codigo;
  formCodigo.requestSubmit();
  await esperar();
}

test('«Validar código» pone el foco en el campo del código', () => {
  clic(menu('validar'));
  assert.equal(pagina.$('[data-panel-vista="validar"]').hidden, false);
  assert.equal(pagina.document.activeElement, formCodigo.elements.codigo);
});

test('sin código avisa «Falta el código» y no consulta al servidor', async () => {
  pagina.alertas.length = 0;
  await consultar('   ');
  assert.deepEqual(pagina.alertas.map(a => [a.icon, a.title]), [['warning', 'Falta el código']]);
  assert.equal(pagina.llamadas.filter(l => l.ruta.startsWith('/api/local/recojos/')).length, 0);
});

test('un código pagado (en minúsculas) muestra el pedido listo para entregar', async () => {
  pagina.servidor['GET /api/local/recojos/805949F6-20C9-42'] = COMPRA;
  await consultar('  805949f6-20c9-42 ');
  assert.equal(pagina.llamadasA('GET', '/api/local/recojos/805949F6-20C9-42').length, 1);
  const texto = resultado();
  assert.match(texto, /805949F6-20C9-42/);
  assert.match(texto, /Pagado · listo para entregar/);
  assert.match(texto, /1 × Pequeño.*S\/ 8\.00/);
  assert.match(texto, /1 × Grande.*S\/ 15\.00/);
  assert.match(texto, /Valeria Quispe/);
  assert.match(texto, /S\/ 23\.00/);
  assert.match(texto, /Yape/);
  assert.match(texto, /Horario de recojo/);
  assert.match(texto, /Entrega el pedido al cliente y luego confirma/);
  assert.ok(boton('Confirmar entrega'));
});

test('confirmar la entrega marca el código como usado y actualiza la ganancia', async () => {
  pagina.servidor['POST /api/local/recojos/805949F6-20C9-42/confirmar'] = { ...COMPRA, estado: 'RECOGIDA' };
  pagina.alertas.length = 0;
  const antes = pagina.llamadasA('GET', '/api/local/resumen').length;
  clic(boton('Confirmar entrega'));
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/local/recojos/805949F6-20C9-42/confirmar').length, 1);
  const texto = resultado();
  assert.match(texto, /Entrega confirmada correctamente/);
  assert.match(texto, /Venta S\/ 23\.00 − comisión de Sazloop S\/ 2\.30 = ganancia neta S\/ 20\.70/);
  assert.match(texto, /Entregado/);
  assert.equal(boton('Confirmar entrega'), undefined);
  assert.equal(formCodigo.elements.codigo.value, '');
  assert.ok(titulos().includes('Entrega confirmada'));
  assert.equal(pagina.llamadasA('GET', '/api/local/resumen').length, antes + 1);
});

test('un código ya entregado indica que no puede volver a usarse', async () => {
  pagina.servidor['GET /api/local/recojos/805949F6-20C9-42'] = { ...COMPRA, estado: 'RECOGIDA' };
  await consultar('805949F6-20C9-42');
  assert.match(resultado(), /Este pedido ya fue entregado; el código no puede volver a usarse/);
  assert.equal(pagina.$('#resultado-codigo .acciones-formulario').children.length, 0);
});

test('si el servidor rechaza la confirmación (409), lo muestra y permite reintentar', async () => {
  pagina.servidor['GET /api/local/recojos/805949F6-20C9-42'] = COMPRA;
  pagina.servidor['POST /api/local/recojos/805949F6-20C9-42/confirmar'] = respuesta(409, { message: 'El código ya fue usado o la compra no está confirmada' });
  await consultar('805949F6-20C9-42');
  pagina.alertas.length = 0;
  const confirmarBoton = boton('Confirmar entrega');
  clic(confirmarBoton);
  await esperar();
  assert.match(resultado(), /El código ya fue usado o la compra no está confirmada/);
  assert.ok(titulos().includes('No se pudo confirmar la entrega'));
  assert.equal(confirmarBoton.disabled, false);
});

test('un pedido por delivery se entrega al repartidor', async () => {
  const delivery = {
    ...COMPRA, codigoRecojo: 'D1E2L3', modalidad: 'DELIVERY', empresaDelivery: 'RAPPI',
    entregaDireccion: 'Av. Larco 123', entregaDistrito: 'Miraflores', entregaReferencia: 'frente al parque', medioPago: 'TARJETA_DEBITO'
  };
  pagina.servidor['GET /api/local/recojos/D1E2L3'] = delivery;
  pagina.servidor['POST /api/local/recojos/D1E2L3/confirmar'] = { ...delivery, estado: 'RECOGIDA' };
  await consultar('d1e2l3');
  let texto = resultado();
  assert.match(texto, /DELIVERY/);
  assert.match(texto, /Delivery: Rappi – Av\. Larco 123, Miraflores \(frente al parque\)/);
  assert.match(texto, /Entregar al repartidor/);
  assert.match(texto, /Entrega el pedido al repartidor de Rappi y luego confirma/);
  assert.match(texto, /Tarjeta de débito/);
  clic(boton('Entregado al repartidor'));
  await esperar();
  texto = resultado();
  assert.match(texto, /Entregado al repartidor correctamente/);
});

test('una reserva en efectivo se confirma al recibir el pago', async () => {
  const efectivo = { ...COMPRA, codigoRecojo: 'EFE123', estado: 'PENDIENTE_PAGO', medioPago: 'EFECTIVO' };
  pagina.servidor['GET /api/local/recojos/EFE123'] = efectivo;
  pagina.servidor['POST /api/local/efectivo/EFE123/confirmar'] = { ...efectivo, estado: 'RECOGIDA' };
  await consultar('efe123');
  assert.match(resultado(), /Efectivo pendiente de cobro/);
  assert.match(resultado(), /Cobra el total en efectivo antes de confirmar/);
  assert.match(resultado(), /Efectivo/);
  clic(boton('Recibí el efectivo y entregué el pedido'));
  await esperar();
  assert.equal(pagina.llamadasA('POST', '/api/local/efectivo/EFE123/confirmar').length, 1);
  assert.match(resultado(), /Entrega confirmada correctamente/);
});

test('un pedido cancelado o con estado desconocido no tiene entrega pendiente', async () => {
  pagina.servidor['GET /api/local/recojos/CAN1'] = { ...COMPRA, codigoRecojo: 'CAN1', estado: 'CANCELADA', lineas: undefined, cliente: null };
  await consultar('CAN1');
  assert.match(resultado(), /Cancelado por el cliente/);
  assert.match(resultado(), /Este pedido no tiene una entrega pendiente/);
  assert.match(resultado(), /Cliente—/);
  pagina.servidor['GET /api/local/recojos/RARO1'] = { ...COMPRA, codigoRecojo: 'RARO1', estado: 'EN_DISPUTA', medioPago: 'PLIN' };
  await consultar('RARO1');
  assert.match(resultado(), /EN_DISPUTA/);
  assert.match(resultado(), /PLIN/);
});

test('un código desconocido (404) avisa que no se encontró', async () => {
  pagina.alertas.length = 0;
  await consultar('NOEXISTE');
  assert.equal(pagina.llamadasA('GET', '/api/local/recojos/NOEXISTE').length, 1);
  assert.deepEqual(pagina.alertas.map(a => a.title), ['No encontramos ese código']);
  assert.match(resultado(), /No encontramos ese código en este local/);
});

test('un error del servidor al consultar muestra el motivo', async () => {
  pagina.servidor['GET /api/local/recojos/ERR500'] = respuesta(500, { message: 'falló' });
  pagina.alertas.length = 0;
  const errorOriginal = console.error;
  console.error = () => {};
  try { await consultar('ERR500'); } finally { console.error = errorOriginal; }
  assert.deepEqual(pagina.alertas.map(a => a.title), ['No se pudo consultar el código']);
  assert.match(resultado(), /No se pudo consultar el código/);
  assert.match(resultado(), /error inesperado en el servidor \(código 500\)/);
});

test('sin conexión al consultar pide revisar la conexión', async () => {
  pagina.servidor['GET /api/local/recojos/REDX'] = new TypeError('Failed to fetch');
  pagina.alertas.length = 0;
  await consultar('REDX');
  assert.deepEqual(pagina.alertas.map(a => a.title), ['Sin conexión']);
  assert.match(resultado(), /Revisa tu conexión e intenta de nuevo/);
});

// ---------- Cerrar sesión ----------

test('cerrar sesión borra la sesión del local y vuelve al acceso', async () => {
  clic(pagina.$('#salir'));
  await esperar();
  assert.equal(pagina.llamadasA('DELETE', '/api/local/sesion').length, 1);
  assert.deepEqual(pagina.navegaciones, ['acceso-locales.html']);
});

test('cerrar sesión aunque la sesión ya no exista también vuelve al acceso', async () => {
  pagina.servidor['DELETE /api/local/sesion'] = respuesta(401, { message: 'Sin sesión' });
  clic(pagina.$('#salir'));
  await esperar();
  assert.deepEqual(pagina.navegaciones, ['acceso-locales.html', 'acceso-locales.html']);
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
