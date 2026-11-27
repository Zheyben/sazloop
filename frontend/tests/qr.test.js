// Compra presencial por QR (app/qr.html + qr.js): elegir unidades, pagar con Yape, tarjeta o efectivo.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { abrirPagina, esperar, clic, respuesta, CONSUMIDOR } from './navegador.js';

const OFERTA = {
  id: 10, establecimientoId: 2, establecimiento: 'Aurora Miraflores', distrito: 'Miraflores',
  pack: 'Pack sorpresa de panadería', descripcion: 'Panes del día', posiblesProductos: 'Croissants, baguettes',
  inicioRecojo: '2026-11-28T18:00:00', finRecojo: '2026-11-28T21:00:00', estado: 'PUBLICADA',
  variantes: [
    { id: 101, tamanio: 'MEDIANO', precioReferencial: 30, precioDescuento: 14, stockDisponible: 2 },
    { id: 102, tamanio: 'GRANDE', precioReferencial: 45, precioDescuento: 20, stockDisponible: 0 }
  ]
};

const pagina = abrirPagina('qr.html', {
  buscar: '?oferta=10&token=tok-123',
  servidor: {
    'GET /api/sesion': CONSUMIDOR,
    'GET /api/ofertas/10': OFERTA
  }
});
await import('../app/qr.js');
await esperar();

const form = () => pagina.$('#formQr');
const campo = nombre => form().elements[nombre];
const mensaje = () => pagina.$('#mensajeQr').textContent;
function escribir(entrada, valor) {
  entrada.value = valor;
  entrada.dispatchEvent(new pagina.window.Event('input', { bubbles: true }));
  entrada.dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
}
function elegirMedio(valor) {
  const radio = pagina.$(`#metodosQr input[value="${valor}"]`);
  radio.checked = true;
  radio.dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
}
function aceptarTerminos(marcar = true) {
  campo('terminos').checked = marcar;
  campo('terminos').dispatchEvent(new pagina.window.Event('change', { bubbles: true }));
}

test('muestra la oferta y sus variantes; la variante sin stock queda bloqueada', () => {
  const oferta = pagina.$('#ofertaQr').textContent;
  assert.match(oferta, /Aurora Miraflores · Miraflores/);
  assert.match(oferta, /Pack sorpresa de panadería/);
  assert.match(oferta, /Panes del día/);
  assert.match(oferta, /PODRÍA INCLUIR.*Croissants, baguettes/);
  assert.equal(form().hidden, false);
  assert.match(pagina.$('#variantesQr').textContent, /Mediano.*S\/ 14\.00.*S\/ 30\.00.*2 disponibles/);
  assert.equal(campo('v102').disabled, true);
  assert.equal(pagina.$('#totalQr').textContent, 'S/ 0.00');
  assert.equal(pagina.$('#tarjetaQr').hidden, true);
  assert.equal(pagina.$('#confirmarQr').disabled, true);
});

test('los botones + y − cambian la cantidad sin pasar del stock y recalculan el total', () => {
  const mas = pagina.$$('#variantesQr .detalle-mas')[0];
  clic(mas); clic(mas); clic(mas);
  assert.equal(campo('v101').value, '2');
  assert.equal(pagina.$('#totalQr').textContent, 'S/ 28.00');
  assert.ok(pagina.$('#variantesQr .detalle-variante').classList.contains('seleccionada'));
  clic(pagina.$$('#variantesQr .detalle-menos')[0]);
  assert.equal(campo('v101').value, '1');
  assert.equal(pagina.$('#totalQr').textContent, 'S/ 14.00');
});

test('sin validar el pago no deja confirmar', async () => {
  pagina.alertas.length = 0;
  // Los campos de Yape vacíos frenan el envío normal; se envía directo para probar la revisión del código.
  form().dispatchEvent(new pagina.window.Event('submit', { bubbles: true, cancelable: true }));
  await esperar();
  assert.equal(mensaje(), 'Revisa el pago y las cantidades.');
  assert.equal(pagina.ultimaAlerta().title, 'Revisa el pago');
  assert.equal(pagina.llamadas.filter(l => l.metodo === 'POST').length, 0);
});

test('Yape: valida el número y el código', () => {
  escribir(campo('telefonoYape'), '123');
  clic(pagina.$('#siguienteYapeQr'));
  assert.equal(mensaje(), 'Ingresa un número peruano que empiece por 9.');
  assert.equal(pagina.$('#codigoYapeQr').hidden, true);

  escribir(campo('telefonoYape'), '987654321');
  clic(pagina.$('#siguienteYapeQr'));
  assert.equal(pagina.$('#codigoYapeQr').hidden, false);
  assert.equal(mensaje(), '');

  escribir(campo('codigoYape'), 'abc');
  clic(pagina.$('#validarYapeQr'));
  assert.equal(mensaje(), 'Ingresa seis dígitos.');

  escribir(campo('codigoYape'), '654321');
  clic(pagina.$('#validarYapeQr'));
  assert.equal(mensaje(), 'Yape validado. Acepta los términos para continuar.');
  assert.equal(pagina.ultimaAlerta().toast, true);
  assert.equal(campo('telefonoYape').readOnly, true);
  assert.equal(pagina.$('#confirmarQr').disabled, true);
  aceptarTerminos();
  assert.equal(pagina.$('#confirmarQr').disabled, false);
});

test('cambiar a tarjeta reinicia el pago y rechaza una tarjeta vencida', () => {
  elegirMedio('TARJETA_DEBITO');
  assert.equal(pagina.$('#tarjetaQr').hidden, false);
  assert.equal(pagina.$('#yapeQr').hidden, true);
  assert.equal(campo('terminos').checked, false);
  assert.equal(pagina.$('#confirmarQr').disabled, true);

  escribir(campo('numeroTarjeta'), '4111 1111 1111 1111');
  escribir(campo('titular'), 'Valeria Quispe');
  escribir(campo('vencimiento'), '2020-01');
  escribir(campo('cvv'), '123');
  clic(pagina.$('#validarTarjetaQr'));
  assert.equal(mensaje(), 'Revisa el número, titular, vencimiento y CVV o CVC.');
  assert.equal(campo('numeroTarjeta').readOnly, false);
});

test('una tarjeta vigente queda validada', () => {
  aceptarTerminos();
  assert.equal(pagina.$('#confirmarQr').disabled, true, 'sin validar no se habilita');
  escribir(campo('vencimiento'), '2031-06');
  clic(pagina.$('#validarTarjetaQr'));
  assert.equal(mensaje(), 'Tarjeta validada. Acepta los términos para continuar.');
  assert.equal(campo('cvv').readOnly, true);
  assert.equal(pagina.$('#confirmarQr').disabled, false, 'los términos ya estaban aceptados');
});

test('si el servidor rechaza la compra con tarjeta, muestra el motivo', async () => {
  pagina.alertas.length = 0;
  pagina.servidor['POST /api/compras/presenciales'] = respuesta(409, { message: 'El QR ya venció' });
  form().requestSubmit();
  await esperar();
  const [llamada] = pagina.llamadasA('POST', '/api/compras/presenciales');
  assert.deepEqual(llamada.cuerpo, { token: 'tok-123', compra: {
    ofertaId: 10, medioPago: 'TARJETA_DEBITO', lineas: [{ varianteId: 101, cantidad: 1 }] } });
  assert.equal(mensaje(), 'El QR ya venció');
  assert.ok(pagina.alertas.some(a => a.title === 'No se pudo confirmar la compra' && a.text === 'El QR ya venció'));
  assert.equal(pagina.$('#confirmarQr').disabled, false);
});

test('sin conexión avisa que revise su internet', async () => {
  pagina.servidor['POST /api/compras/presenciales'] = new TypeError('Failed to fetch');
  form().requestSubmit();
  await esperar();
  assert.equal(mensaje(), 'Sin conexión. Revisa tu internet e intenta de nuevo.');
  assert.equal(pagina.ultimaAlerta().title, 'Sin conexión');
});

test('el efectivo no pide validación y reserva el pedido', async () => {
  pagina.alertas.length = 0;
  elegirMedio('EFECTIVO');
  assert.equal(pagina.$('#ayudaEfectivoQr').hidden, false);
  assert.equal(pagina.$('#tarjetaQr').hidden, true);
  aceptarTerminos();
  assert.equal(pagina.$('#confirmarQr').disabled, false);
  clic(pagina.$$('#variantesQr .detalle-mas')[0]);
  pagina.servidor['POST /api/compras/presenciales/efectivo'] = {
    reservaId: 9, codigoRecojo: 'EFEC-99', total: 28, venceEn: '2026-11-27T12:10:00', estado: 'PENDIENTE_PAGO'
  };
  form().requestSubmit();
  await esperar();
  const [llamada] = pagina.llamadasA('POST', '/api/compras/presenciales/efectivo');
  assert.deepEqual(llamada.cuerpo, { token: 'tok-123', compra: {
    ofertaId: 10, medioPago: 'EFECTIVO', lineas: [{ varianteId: 101, cantidad: 2 }] } });
  assert.equal(form().hidden, true);
  const resultado = pagina.$('#resultadoQr');
  assert.equal(resultado.className, 'compra-confirmada');
  assert.match(resultado.textContent, /Entrega el efectivo al trabajador.*Código: EFEC-99.*Total: S\/ 28\.00.*La reserva vence: /);
  assert.equal(resultado.querySelector('a').getAttribute('href'), 'compras.html');
  const exito = pagina.alertas.find(a => a.title === 'Reserva lista');
  assert.equal(exito.text, 'Entrega el efectivo al trabajador. Tu código es EFEC-99.');
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(pagina.errores, []);
});
