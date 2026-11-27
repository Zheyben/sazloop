// Pantalla «Productos» del consumidor (app/catalogo.html + catalogo.js + ofertas-portal.js):
// filtros, «Cerca de mí», detalle del pack y carrito.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, respuesta, CONSUMIDOR } from './navegador.js';

// Fecha local sin zona horaria, como la manda el backend (LocalDateTime).
function fechaLocal(desfaseHoras) {
  const d = new Date(Date.now() + desfaseHoras * 3_600_000);
  const dos = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}T${dos(d.getHours())}:${dos(d.getMinutes())}:00`;
}

function oferta(id, cambios = {}) {
  return {
    id, establecimientoId: id * 10, establecimiento: `Panadería ${id}`, distrito: 'Ate', direccion: 'Av. Central 123',
    calificacion: 4.5, pack: 'Pack Dulce', descripcion: 'Panes y postres del día',
    posiblesProductos: 'Croissants, pan francés', alergenos: ['Gluten', 'Huevo'],
    restricciones: ['Apto para vegetarianos', 'Sin azúcar añadida'],
    inicioRecojo: fechaLocal(48), finRecojo: fechaLocal(49), limiteDefinitivo: fechaLocal(47),
    estado: 'PUBLICADA', distanciaKm: null,
    variantes: [
      { id: id * 10 + 1, tamanio: 'PEQUENO', precioReferencial: 20, precioDescuento: 8, ahorro: 12, stockDisponible: 2 },
      { id: id * 10 + 2, tamanio: 'GRANDE', precioReferencial: 30, precioDescuento: 12, ahorro: 18, stockDisponible: 0 }
    ],
    ...cambios
  };
}

// Oferta 1: recojo hoy hasta mañana (cambia de día). Oferta 2: sin stock y sin calificación.
const OFERTA_1 = oferta(1, { inicioRecojo: fechaLocal(0), finRecojo: fechaLocal(26) });
const OFERTA_2 = oferta(2, {
  pack: 'Combo', calificacion: null, descripcion: '', alergenos: [], restricciones: [],
  variantes: [{ id: 21, tamanio: 'MEDIANO', precioReferencial: 15, precioDescuento: 6, ahorro: 9, stockDisponible: 0 }]
});

let listado = () => [OFERTA_1, OFERTA_2];
const pagina = abrirPagina('catalogo.html', {
  buscar: '?distrito=Ate&otro=ignorado',
  servidor: {
    'GET /api/sesion': CONSUMIDOR,
    'GET /api/notificaciones': [],
    'GET /api/compras/mias': [],
    'GET /api/ofertas': (cuerpo, llamada) => listado(llamada),
    'GET /api/ofertas/1': OFERTA_1,
    'GET /api/ofertas/2': respuesta(500, { message: 'Error interno' })
  }
});
await import('../app/portal-main.js');
await esperar();

const formulario = () => pagina.$('#filtros');
const tarjetas = () => pagina.$$('#lista .tarjeta-pack');
const ultimaBusqueda = () => pagina.llamadasA('GET', '/api/ofertas').at(-1);
function enviar(form) {
  form.dispatchEvent(new pagina.window.Event('submit', { bubbles: true, cancelable: true }));
}
function escribir(campo, valor) {
  campo.value = valor;
  campo.dispatchEvent(new pagina.window.Event('input', { bubbles: true }));
}
function marcarCerca(marcada) {
  const casilla = formulario().elements.cerca;
  casilla.checked = marcada;
  casilla.dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
}
function permitirUbicacion(coords) {
  Object.defineProperty(pagina.window.navigator, 'geolocation', {
    configurable: true,
    value: { getCurrentPosition: (exito, fallo) => (coords ? exito({ coords }) : fallo({ code: 1 })) }
  });
}

test('toma los filtros permitidos de la dirección de la página al cargar', () => {
  assert.equal(formulario().elements.distrito.value, 'Ate');
  assert.equal(ultimaBusqueda().buscar, '?distrito=Ate');
  assert.equal(pagina.$('#contador-filtros').textContent, '1');
  assert.equal(pagina.$('#contador-filtros').hidden, false);
});

test('muestra una tarjeta por oferta con precio, descuento y horario', () => {
  assert.equal(tarjetas().length, 2);
  const [primera, segunda] = tarjetas();
  assert.match(primera.textContent, /Panadería 1 · Ate/);
  assert.match(primera.textContent, /2 uds\./);
  assert.match(primera.textContent, /★ {2}4\.5/);
  assert.match(primera.textContent, /S\/ 8\.00/);
  assert.match(primera.textContent, /−60 %/);
  assert.match(primera.querySelector('.pack-horario').textContent, /^Recojo hoy · .+ – .+ · .+/);
  assert.equal(primera.querySelector('h3 em').textContent, 'Dulce');
  assert.equal(primera.querySelector('.pack-distancia'), null, 'sin «Cerca de mí» no se muestra distancia');
  assert.match(segunda.textContent, /Sin stock/);
  assert.match(segunda.textContent, /★ {2}-/);
  assert.equal(segunda.querySelector('h3').textContent, 'Combo');
});

test('el botón «Filtros» abre y cierra el panel', () => {
  const boton = pagina.$('#alternar-filtros');
  clic(boton);
  assert.equal(pagina.$('#panel-filtros').hidden, false);
  assert.equal(boton.getAttribute('aria-expanded'), 'true');
  assert.equal(pagina.document.activeElement, formulario().elements.distrito);
  clic(boton);
  assert.equal(pagina.$('#panel-filtros').hidden, true);
  assert.equal(boton.getAttribute('aria-expanded'), 'false');
});

test('aplicar filtros arma la consulta y la guarda en la dirección de la página', async () => {
  clic(pagina.$('#alternar-filtros'));
  const form = formulario();
  form.elements.distrito.value = ' Miraflores ';
  form.elements.direccion.value = 'Larco';
  form.elements.precioMaximo.value = '15';
  form.elements.calificacionMinima.value = '4';
  enviar(form);
  await esperar();
  const params = new URLSearchParams(ultimaBusqueda().buscar);
  assert.equal(params.get('distrito'), 'Miraflores');
  assert.equal(params.get('direccion'), 'Larco');
  assert.equal(params.get('precioMaximo'), '15');
  assert.equal(params.get('calificacionMinima'), '4');
  assert.equal(params.has('lat'), false);
  assert.equal(pagina.window.location.search, '?distrito=Miraflores&direccion=Larco&precioMaximo=15&calificacionMinima=4');
  assert.equal(pagina.$('#panel-filtros').hidden, true);
  assert.equal(pagina.$('#contador-filtros').textContent, '4');
});

test('sin filtros la dirección de la página queda limpia y el contador se oculta', async () => {
  const form = formulario();
  for (const nombre of ['distrito', 'direccion', 'precioMaximo', 'calificacionMinima']) form.elements[nombre].value = '';
  enviar(form);
  await esperar();
  assert.equal(ultimaBusqueda().buscar, '');
  assert.equal(pagina.window.location.search, '');
  assert.equal(pagina.$('#contador-filtros').hidden, true);
});

test('«Cerca de mí» sin permiso de ubicación avisa y desmarca la casilla', async () => {
  permitirUbicacion(null);
  pagina.alertas.length = 0;
  marcarCerca(true);
  await esperar();
  assert.equal(formulario().elements.cerca.checked, false);
  assert.equal(formulario().elements.cerca.disabled, false);
  assert.ok(pagina.alertas.some(a => a.title === 'Activa la ubicación para usar este filtro' && a.icon === 'info'));
  enviar(formulario());
  await esperar();
  assert.equal(new URLSearchParams(ultimaBusqueda().buscar).has('lat'), false);
});

test('«Cerca de mí» con permiso manda la ubicación y muestra la distancia', async () => {
  permitirUbicacion({ latitude: -12.0463731, longitude: -77.042754 });
  listado = llamada => (new URLSearchParams(llamada.buscar).has('lat')
    ? [oferta(1, { distanciaKm: 1.234 }), oferta(3, { distanciaKm: 0.05 }), oferta(4, { distanciaKm: null })]
    : [OFERTA_1]);
  marcarCerca(true);
  await esperar();
  assert.equal(formulario().elements.cerca.checked, true);
  enviar(formulario());
  await esperar();
  const params = new URLSearchParams(ultimaBusqueda().buscar);
  assert.equal(params.get('lat'), '-12.046373');
  assert.equal(params.get('lng'), '-77.042754');
  assert.equal(pagina.window.location.search, '', 'la ubicación no se guarda en la dirección de la página');
  assert.equal(pagina.$('#contador-filtros').textContent, '1');
  const distancias = pagina.$$('.pack-distancia').map(p => p.textContent);
  assert.deepEqual(distancias, ['a 1,2 km', 'a menos de 0,1 km', 'Distancia no disponible']);
  assert.ok(pagina.$$('.pack-distancia')[2].classList.contains('sin-dato'));
});

test('al desmarcar «Cerca de mí» se deja de mandar la ubicación', async () => {
  marcarCerca(false);
  enviar(formulario());
  await esperar();
  assert.equal(new URLSearchParams(ultimaBusqueda().buscar).has('lat'), false);
  assert.equal(pagina.$$('.pack-distancia').length, 0);
});

test('sin resultados muestra el mensaje para cambiar la búsqueda', async () => {
  listado = () => [];
  enviar(formulario());
  await esperar();
  assert.match(pagina.$('#lista').textContent, /No hay packs con esos filtros\. Prueba cambiando la búsqueda\./);
});

test('si el servidor falla, vacía la lista y avisa', async () => {
  listado = () => respuesta(500, { message: 'Error' });
  pagina.alertas.length = 0;
  enviar(formulario());
  await esperar();
  assert.equal(pagina.$('#lista').children.length, 0);
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudieron cargar las ofertas'));
  assert.match(pagina.$('#mensaje').textContent, /error inesperado en el servidor/);
});

test('«Ver pack» abre el detalle con alérgenos, dieta y tamaños', async () => {
  listado = () => [OFERTA_1, OFERTA_2];
  enviar(formulario());
  await esperar();
  clic(tarjetas()[0].querySelector('.pack-accion'));
  await esperar();
  const detalle = pagina.$('#detalle');
  assert.equal(detalle.open, true);
  assert.equal(pagina.llamadasA('GET', '/api/ofertas/1').length, 1);
  assert.match(detalle.textContent, /Panes y postres del día/);
  assert.match(detalle.textContent, /Av\. Central 123, Ate/);
  assert.deepEqual(pagina.$$('.detalle-chip').map(c => c.textContent), ['Gluten', 'Huevo']);
  const dieta = pagina.$('.detalle-dieta').textContent;
  assert.match(dieta, /Apto para vegetarianos/);
  assert.match(dieta, /No apto para veganos/);
  assert.match(dieta, /Sin azúcar añadida/);
  assert.equal(pagina.$('.detalle-total').textContent, 'S/ 0.00');
  assert.equal(pagina.$('input[name="v12"]').disabled, true, 'el tamaño sin stock no se puede elegir');
});

test('los botones + y − cambian la cantidad sin pasar el stock', () => {
  const mas = pagina.$$('.detalle-mas')[0];
  const menos = pagina.$$('.detalle-menos')[0];
  const campo = pagina.$('input[name="v11"]');
  clic(menos);
  assert.equal(campo.value, '0');
  clic(mas); clic(mas); clic(mas);
  assert.equal(campo.value, '2');
  assert.equal(pagina.$('.detalle-total').textContent, 'S/ 16.00');
  assert.ok(pagina.$$('.detalle-variante')[0].classList.contains('seleccionada'));
  clic(menos);
  assert.equal(campo.value, '1');
  assert.equal(pagina.$('.detalle-total').textContent, 'S/ 8.00');
});

test('sin unidades elegidas pide al menos una', () => {
  pagina.alertas.length = 0;
  escribir(pagina.$('input[name="v11"]'), '0');
  enviar(pagina.$('#formulario-detalle-pack'));
  assert.equal(pagina.$('.detalle-aviso').textContent, 'Elige al menos una unidad válida.');
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudo añadir' && a.text === 'Elige al menos una unidad válida.'));
  assert.equal(pagina.$('#detalle').open, true);
});

test('una cantidad mayor al stock pide revisar las cantidades', () => {
  pagina.alertas.length = 0;
  escribir(pagina.$('input[name="v11"]'), '5');
  assert.equal(pagina.$('.detalle-total').textContent, 'Revisa las cantidades');
  enviar(pagina.$('#formulario-detalle-pack'));
  assert.equal(pagina.$('.detalle-aviso').textContent, 'Revisa las cantidades disponibles.');
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudo añadir' && a.text === 'Revisa las cantidades disponibles.'));
});

test('añadir al carrito guarda el pedido en la sesión y cierra el detalle', async () => {
  pagina.alertas.length = 0;
  escribir(pagina.$('input[name="v11"]'), '2');
  enviar(pagina.$('#formulario-detalle-pack'));
  await esperar();
  const carrito = JSON.parse(pagina.window.sessionStorage.getItem(`sazloop-carrito-${CONSUMIDOR.id}`));
  assert.equal(carrito.length, 1);
  assert.equal(carrito[0].ofertaId, 1);
  assert.deepEqual(carrito[0].lineas, [{ varianteId: 11, cantidad: 2 }]);
  assert.equal(typeof carrito[0].clave, 'string');
  assert.equal(pagina.$('#detalle').open, false);
  assert.equal(pagina.$('#contador-carrito').textContent, '1');
  assert.ok(pagina.alertas.some(a => a.icon === 'success' && a.title === 'Añadido al carrito'
    && a.text === 'Pack Dulce ya está en tu carrito.'));
});

test('no deja sumar más unidades que las disponibles contando el carrito', async () => {
  clic(tarjetas()[0].querySelector('.pack-accion'));
  await esperar();
  pagina.alertas.length = 0;
  clic(pagina.$$('.detalle-mas')[0]);
  enviar(pagina.$('#formulario-detalle-pack'));
  assert.equal(pagina.$('.detalle-aviso').textContent, 'La cantidad supera las unidades disponibles.');
  assert.ok(pagina.alertas.some(a => a.text === 'La cantidad supera las unidades disponibles.'));
  const carrito = JSON.parse(pagina.window.sessionStorage.getItem(`sazloop-carrito-${CONSUMIDOR.id}`));
  assert.equal(carrito.length, 1, 'el carrito no cambia');
});

test('el botón de cerrar cierra el detalle', () => {
  clic(pagina.$('.detalle-cerrar'));
  assert.equal(pagina.$('#detalle').open, false);
});

test('si no se puede abrir un pack, avisa', async () => {
  pagina.alertas.length = 0;
  clic(tarjetas()[1].querySelector('.pack-accion'));
  await esperar();
  assert.equal(pagina.$('#detalle').open, false);
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudo abrir el pack'));
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
