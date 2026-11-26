// Pruebas unitarias de app/comun.js con el ejecutor de pruebas de Node (sin instalar nada).
// Ejecutar desde frontend/:  node --test tests/
import { test, beforeEach } from 'node:test';
import assert from 'node:assert/strict';

// --- Dobles mínimos del navegador: document, Swal, alert y fetch ---
let ultimaAlerta;
let zonaMensaje;
let alertas;

globalThis.document = {
  querySelector: selector => (selector === '#mensaje' ? zonaMensaje : null),
  createElement: etiqueta => ({ etiqueta, hijos: [], append(...nodos) { this.hijos.push(...nodos); } })
};
globalThis.alert = texto => alertas.push(texto);

const comun = await import('../app/comun.js');

beforeEach(() => {
  ultimaAlerta = null;
  zonaMensaje = null;
  alertas = [];
  globalThis.Swal = { fire: async opciones => { ultimaAlerta = opciones; return { isConfirmed: true }; } };
});

function respuesta(status, cuerpo) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => {
      if (cuerpo === undefined) throw new Error('sin cuerpo');
      return cuerpo;
    }
  };
}

// --- Formatos ---
test('dinero muestra soles con dos decimales', () => {
  assert.equal(comun.dinero(14), 'S/ 14.00');
  assert.equal(comun.dinero('9.5'), 'S/ 9.50');
});

test('conPunto agrega el punto solo cuando falta', () => {
  assert.equal(comun.conPunto('Documento vencido'), 'Documento vencido.');
  assert.equal(comun.conPunto('  Listo.  '), 'Listo.');
  assert.equal(comun.conPunto('¿Seguro?'), '¿Seguro?');
});

test('esDelivery distingue la modalidad de la compra', () => {
  assert.equal(comun.esDelivery({ modalidad: 'DELIVERY' }), true);
  assert.equal(comun.esDelivery({ modalidad: 'RECOJO' }), false);
  assert.equal(comun.esDelivery(null), false);
});

test('textoDelivery arma la entrega con y sin referencia', () => {
  const compra = { empresaDelivery: 'PEDIDOSYA', entregaDireccion: 'Av. Larco 123', entregaDistrito: 'Miraflores' };
  assert.equal(comun.textoDelivery(compra), 'PedidosYa – Av. Larco 123, Miraflores');
  assert.equal(comun.textoDelivery({ ...compra, entregaReferencia: 'frente al parque' }),
    'PedidosYa – Av. Larco 123, Miraflores (frente al parque)');
  assert.equal(comun.textoDelivery({ ...compra, empresaDelivery: 'OTRA' }), 'OTRA – Av. Larco 123, Miraflores');
});

test('fechaHora devuelve un texto legible', () => {
  assert.equal(typeof comun.fechaHora('2026-11-26T18:08:00'), 'string');
});

// --- Tarjeta ---
test('tarjetaVigente acepta el mes actual y rechaza el anterior', () => {
  const hoy = new Date();
  const mes = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  const anterior = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
  const siguienteAnio = new Date(hoy.getFullYear() + 1, hoy.getMonth(), 1);
  assert.equal(comun.tarjetaVigente(mes(hoy)), true);
  assert.equal(comun.tarjetaVigente(mes(siguienteAnio)), true);
  assert.equal(comun.tarjetaVigente(mes(anterior)), false);
  assert.equal(comun.tarjetaVigente(''), false);
});

// --- Errores de red ---
test('esFalloDeRed reconoce los mensajes de los navegadores', () => {
  assert.equal(comun.esFalloDeRed(new Error('Failed to fetch')), true);
  assert.equal(comun.esFalloDeRed('NetworkError when attempting to fetch resource.'), true);
  assert.equal(comun.esFalloDeRed('Load failed'), true);
  assert.equal(comun.esFalloDeRed(new Error('Sin stock')), false);
  assert.equal(comun.esFalloDeRed(null), false);
});

// --- Elementos sin innerHTML ---
test('el crea el elemento con sus propiedades y descarta hijos vacíos', () => {
  const nodo = comun.el('p', { textContent: 'Hola' }, 'a', null, undefined, 'b');
  assert.equal(nodo.etiqueta, 'p');
  assert.equal(nodo.textContent, 'Hola');
  assert.deepEqual(nodo.hijos, ['a', 'b']);
});

// --- Avisos con SweetAlert2 ---
test('notificar usa SweetAlert2 con el botón según el tipo', async () => {
  await comun.notificar({ icon: 'success', title: 'Listo' });
  assert.equal(ultimaAlerta.confirmButtonText, 'Listo');
  await comun.notificar({ icon: 'error', title: 'Falló', confirmButtonText: 'Cerrar' });
  assert.equal(ultimaAlerta.confirmButtonText, 'Cerrar');
  await comun.notificar();
  assert.equal(ultimaAlerta.icon, 'info');
  assert.equal(ultimaAlerta.confirmButtonText, 'Entendido');
});

test('sin SweetAlert2, notificar usa alert salvo en los mensajes de éxito', async () => {
  delete globalThis.Swal;
  assert.deepEqual(await comun.notificar({ icon: 'error', title: 'Falló', text: 'Detalle' }), { isConfirmed: true });
  await comun.notificar({ icon: 'success', title: 'Listo' });
  assert.deepEqual(alertas, ['Falló\nDetalle']);
});

test('notificarError muestra «Sin conexión» cuando falla internet', async () => {
  await comun.notificarError(new Error('Failed to fetch'), 'No se pudo guardar');
  assert.equal(ultimaAlerta.title, 'Sin conexión');
  assert.equal(ultimaAlerta.icon, 'error');
});

test('notificarError usa advertencia para errores de datos y error para lo demás', async () => {
  await comun.notificarError('Revisa las cantidades disponibles.');
  assert.equal(ultimaAlerta.icon, 'warning');
  assert.equal(ultimaAlerta.title, 'Revisa la información');

  await comun.notificarError(new Error('Servidor caído'));
  assert.equal(ultimaAlerta.icon, 'error');
  assert.equal(ultimaAlerta.title, 'No se pudo completar');

  await comun.notificarError(null, 'Título propio');
  assert.equal(ultimaAlerta.title, 'Título propio');
  assert.equal(ultimaAlerta.text, 'Ocurrió un error inesperado. Intenta de nuevo.');
});

test('notificarExito y notificarBreve muestran mensajes de éxito', async () => {
  await comun.notificarExito('Compra confirmada', 'Ya tienes tu código.');
  assert.equal(ultimaAlerta.icon, 'success');
  await comun.notificarBreve('success', 'Guardado');
  assert.equal(ultimaAlerta.toast, true);
  delete globalThis.Swal;
  await comun.notificarBreve('error', 'Sin guardar');
  assert.deepEqual(alertas, ['Sin guardar']);
});

test('confirmarAccion devuelve true solo si la persona confirma', async () => {
  assert.equal(await comun.confirmarAccion('¿Cancelar?', 'Se devolverá el 50 %.'), true);
  assert.equal(ultimaAlerta.showCancelButton, true);
  globalThis.Swal = { fire: async () => ({ isConfirmed: false }) };
  assert.equal(await comun.confirmarAccion('¿Cancelar?', 'Texto'), false);
  delete globalThis.Swal;
  globalThis.confirm = () => true;
  assert.equal(await comun.confirmarAccion('¿Cancelar?', 'Texto'), true);
});

test('aviso escribe en la zona de mensajes y muestra la alerta', async () => {
  zonaMensaje = { textContent: '', className: '' };
  comun.aviso('Datos guardados');
  assert.equal(zonaMensaje.textContent, 'Datos guardados');
  assert.equal(zonaMensaje.className, 'ok');
  await Promise.resolve();
  assert.equal(ultimaAlerta.icon, 'success');

  comun.aviso('Failed to fetch', true);
  assert.equal(zonaMensaje.textContent, 'Sin conexión. Revisa tu internet e intenta de nuevo.');
  assert.equal(zonaMensaje.className, 'error');

  ultimaAlerta = null;
  comun.aviso('');
  assert.equal(ultimaAlerta, null);
});

// --- Llamadas al backend ---
test('solicitar devuelve el JSON y manda la cookie de sesión', async () => {
  let llamada;
  globalThis.fetch = async (url, opciones) => { llamada = { url, opciones }; return respuesta(200, { ok: 1 }); };
  assert.deepEqual(await comun.solicitar('/ofertas', { method: 'POST', body: '{}' }), { ok: 1 });
  assert.equal(llamada.url, '/api/ofertas');
  assert.equal(llamada.opciones.credentials, 'include');
  assert.equal(llamada.opciones.headers['Content-Type'], 'application/json');
});

test('solicitar devuelve null cuando no hay contenido (204)', async () => {
  globalThis.fetch = async () => respuesta(204);
  assert.equal(await comun.solicitar('/sesion', { method: 'DELETE' }), null);
});

test('solicitar muestra el motivo de negocio que envía el backend', async () => {
  globalThis.fetch = async () => respuesta(409, { message: 'Ya tienes una reserva en efectivo pendiente' });
  await assert.rejects(comun.solicitar('/compras'), error => {
    assert.equal(error.message, 'Ya tienes una reserva en efectivo pendiente');
    assert.equal(error.status, 409);
    assert.equal(error.ruta, '/compras');
    return true;
  });
});

test('solicitar traduce los mensajes técnicos a texto claro', async () => {
  globalThis.fetch = async () => respuesta(404, { message: 'Not Found' });
  await assert.rejects(comun.solicitar('/ofertas/99'), { message: 'No se encontró lo que buscabas.' });
  globalThis.fetch = async () => respuesta(401);
  await assert.rejects(comun.solicitar('/compras/mias'), { message: 'Necesitas iniciar sesión para hacer esto.' });
  globalThis.fetch = async () => respuesta(418, {});
  await assert.rejects(comun.solicitar('/x'), { message: 'Ocurrió un error inesperado. Intenta de nuevo.' });
});

test('solicitar identifica los errores del servidor (5xx) con su código', async () => {
  const errorOriginal = console.error;
  console.error = () => {};
  try {
    globalThis.fetch = async () => respuesta(500, { message: 'NullPointerException' });
    await assert.rejects(comun.solicitar('/compras'), error => {
      assert.match(error.message, /código 500/);
      return true;
    });
  } finally {
    console.error = errorOriginal;
  }
});
