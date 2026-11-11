// comun.js — piezas compartidas por todas las páginas (módulo ES).
const API = '/api';

// Hace la llamada al backend. Siempre manda la cookie de sesión (credentials: 'include').
export async function solicitar(ruta, opciones = {}) {
  const esFormData = opciones.body instanceof FormData;
  const r = await fetch(API + ruta, {
    ...opciones,
    credentials: 'include',
    headers: { ...(esFormData || !opciones.body ? {} : { 'Content-Type': 'application/json' }), ...opciones.headers }
  });
  if (!r.ok) {
    // Se muestra el motivo de negocio que envía el backend; si no hay uno útil, se traduce el código HTTP.
    const textos = {
      400: 'Revisa los datos ingresados.',
      401: 'Necesitas iniciar sesión para hacer esto.',
      403: 'No tienes permiso para esta acción.',
      404: 'No se encontró lo que buscabas.',
      409: 'La operación no se pudo completar (por ejemplo, sin stock o ya existe).',
      413: 'El archivo supera el tamaño permitido.'
    };
    let motivo = '';
    try { motivo = (await r.json())?.message || ''; } catch { /* respuesta sin cuerpo JSON */ }
    const util = motivo && r.status < 500 && motivo !== 'No message available'
      && !/^(\d{3} [A-Z_]+$|Validation failed|Bad Request|Not Found|Conflict|Unauthorized|Forbidden|Required)/i.test(motivo);
    // Un error del servidor (5xx) se identifica con su código y la ruta, para poder diagnosticarlo.
    const generico = r.status >= 500 ? `Ocurrió un error inesperado en el servidor (código ${r.status}). Intenta de nuevo; si continúa, avisa al equipo.`
      : textos[r.status] || 'Ocurrió un error inesperado. Intenta de nuevo.';
    if (r.status >= 500) console.error('[Sazloop]', opciones.method || 'GET', ruta, '→', r.status, motivo);
    const error = new Error(util ? motivo : generico);
    error.status = r.status;
    error.ruta = ruta;
    throw error;
  }
  return r.status === 204 ? null : r.json();
}

// Crea elementos sin usar innerHTML (así evitamos inyección de HTML con datos del servidor).
export function el(etiqueta, props = {}, ...hijos) {
  const nodo = Object.assign(document.createElement(etiqueta), props);
  nodo.append(...hijos.filter(h => h !== null && h !== undefined));
  return nodo;
}

export const dinero = n => `S/ ${Number(n).toFixed(2)}`;
export const fechaHora = s => new Date(s).toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' });
export const tamanios = { PEQUENO: 'Pequeño', MEDIANO: 'Mediano', GRANDE: 'Grande' };

// Delivery simulado: empresas disponibles y cómo se describe la entrega de una compra.
export const empresasDelivery = { PEDIDOSYA: 'PedidosYa', RAPPI: 'Rappi', INDRIVE: 'inDrive' };
// Agrega el punto final si el texto no termina en puntuación (los motivos vienen sin punto: «… (venció el 09/11/2026)»).
export const conPunto = texto => { const t = String(texto).trim(); return /[.!?…]$/.test(t) ? t : `${t}.`; };
export const esDelivery = compra => compra?.modalidad === 'DELIVERY';
// «PedidosYa – Av. Larco 123, Miraflores (frente al parque)»
export function textoDelivery(compra) {
  const referencia = compra.entregaReferencia ? ` (${compra.entregaReferencia})` : '';
  return `${empresasDelivery[compra.empresaDelivery] || compra.empresaDelivery} – ${compra.entregaDireccion}, ${compra.entregaDistrito}${referencia}`;
}

const COLOR_BOTON = '#183a2b';

export function esFalloDeRed(error) {
  const texto = typeof error === 'string' ? error : error?.message || '';
  return /Failed to fetch|NetworkError|Network request failed|Load failed|internet connection|Network/i.test(texto);
}

function textoDe(error) {
  return typeof error === 'string' ? error : error?.message || 'Ocurrió un error inesperado. Intenta de nuevo.';
}

function textoVisible(texto) {
  return esFalloDeRed(texto) ? 'Sin conexión. Revisa tu internet e intenta de nuevo.' : texto;
}

function dialogoAbierto() {
  return document.querySelector('dialog[open]');
}

/** Alerta modal de SweetAlert2, con el verde de la marca. */
export function notificar({ icon = 'info', title, text, confirmButtonText } = {}) {
  const aviso = [title, text].filter(Boolean).join('\n');
  if (!globalThis.Swal) {
    if (icon === 'success') return Promise.resolve({ isConfirmed: true });
    alert(aviso);
    return Promise.resolve({ isConfirmed: true });
  }
  const dialogo = dialogoAbierto();
  return Swal.fire({
    icon,
    title,
    text,
    confirmButtonText: confirmButtonText || (icon === 'success' ? 'Listo' : 'Entendido'),
    confirmButtonColor: COLOR_BOTON,
    cancelButtonColor: '#6b7280',
    heightAuto: false,
    ...(dialogo ? { target: dialogo } : {})
  });
}

export function notificarError(error, titulo) {
  const texto = textoDe(error);
  if (esFalloDeRed(texto)) {
    return notificar({
      icon: 'error',
      title: 'Sin conexión',
      text: 'Parece que no tienes internet. Revisa tu conexión y vuelve a intentarlo.'
    });
  }
  const esAviso = /revisa|elige|ingresa|incluye|indica|acepta|quita|supera|al menos|no puede|válid/i.test(texto);
  return notificar({
    icon: esAviso ? 'warning' : 'error',
    title: titulo || (esAviso ? 'Revisa la información' : 'No se pudo completar'),
    text: texto
  });
}

export function notificarExito(titulo, texto) {
  return notificar({ icon: 'success', title: titulo, text: texto });
}

/** Confirmación breve, sin tapar el formulario. */
export function notificarBreve(icon, title) {
  if (!globalThis.Swal) return notificar({ icon, title });
  return Swal.fire({
    icon,
    title,
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 2800,
    timerProgressBar: true,
    heightAuto: false
  });
}

/** Pregunta de sí o no. Devuelve true solo si la persona confirma. */
export function confirmarAccion(titulo, texto, aceptar = 'Sí, continuar') {
  if (!globalThis.Swal) return Promise.resolve(confirm(`${titulo}\n\n${texto}`));
  const dialogo = dialogoAbierto();
  return Swal.fire({
    icon: 'warning',
    title: titulo,
    text: texto,
    showCancelButton: true,
    confirmButtonText: aceptar,
    cancelButtonText: 'Volver',
    confirmButtonColor: COLOR_BOTON,
    cancelButtonColor: '#6b7280',
    reverseButtons: true,
    focusCancel: true,
    heightAuto: false,
    ...(dialogo ? { target: dialogo } : {})
  }).then(resultado => resultado.isConfirmed);
}

// Aviso arriba de la página (verde = ok, rojo = error) y alerta según el caso.
export function aviso(texto, esError = false, titulo) {
  const zona = document.querySelector('#mensaje');
  if (zona) {
    zona.textContent = textoVisible(texto);
    zona.className = esError ? 'error' : 'ok';
  }
  if (!texto) return;
  if (esError) notificarError(texto, titulo);
  else notificarExito(titulo || 'Listo', texto);
}

// Una tarjeta sirve hasta el último día de su mes de vencimiento.
// Se usa la fecha local (hora de Perú), no UTC. `mes` llega como "AAAA-MM" desde <input type="month">.
export function tarjetaVigente(mes) {
  const hoy = new Date();
  const actual = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}`;
  return Boolean(mes) && mes >= actual;
}
