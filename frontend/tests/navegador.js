// Navegador simulado para probar las pantallas sin abrir un navegador real.
//
// abrirPagina('notificaciones.html', { servidor }) carga el HTML real de app/, expone window, document,
// location, sessionStorage, etc. como globales (igual que en el navegador) y reemplaza:
//   - fetch: responde con lo que diga `servidor` y guarda cada llamada en `llamadas`;
//   - Swal (SweetAlert2): guarda cada alerta en `alertas` y responde con `confirmar`;
//   - la navegación (location.href = ..., location.replace, location.assign): se guarda en `navegaciones`.
//
// `servidor` es un objeto { 'GET /api/sesion': respuesta, 'POST /api/compras': (cuerpo, llamada) => respuesta }.
// Una respuesta puede ser: un valor (se responde 200 con ese JSON), { status, body }, o una función que
// devuelva cualquiera de los dos. Una ruta que no esté en el objeto responde 404.
import { JSDOM, VirtualConsole } from 'jsdom';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const CARPETA_APP = fileURLToPath(new URL('../app/', import.meta.url));
export const ORIGEN = 'http://localhost:8080';

const GLOBALES = ['window', 'document', 'location', 'history', 'navigator', 'sessionStorage', 'localStorage',
  'HTMLElement', 'HTMLFormElement', 'HTMLInputElement', 'HTMLDialogElement', 'Element', 'Node', 'NodeList',
  'FormData', 'Event', 'MouseEvent', 'KeyboardEvent', 'SubmitEvent', 'CustomEvent', 'DOMParser', 'File', 'Blob',
  'getComputedStyle', 'addEventListener', 'removeEventListener', 'dispatchEvent'];

export function respuesta(status, body) { return { status, body }; }

export function abrirPagina(html, { servidor = {}, buscar = '', hash = '', confirmar = true, geolocalizacion } = {}) {
  const llamadas = [];
  const alertas = [];
  const navegaciones = [];
  const errores = [];

  const consola = new VirtualConsole();
  consola.on('jsdomError', error => {
    if (!/Not implemented: navigation/.test(error.message)) errores.push(error);
  });
  const dom = new JSDOM(readFileSync(CARPETA_APP + html, 'utf8'), {
    url: `${ORIGEN}/app/${html}${buscar}${hash}`,
    pretendToBeVisual: true,
    virtualConsole: consola
  });
  const { window } = dom;

  // <dialog> (jsdom no trae showModal ni close)
  window.HTMLDialogElement.prototype.showModal = function () { this.open = true; };
  window.HTMLDialogElement.prototype.show = function () { this.open = true; };
  window.HTMLDialogElement.prototype.close = function () {
    this.open = false;
    this.dispatchEvent(new window.Event('close'));
  };
  window.scrollTo = () => {};
  window.Element.prototype.scrollIntoView = function () {};

  // Navegación: se registra en vez de cambiar de página.
  const ubicacion = window.location;
  const registrar = destino => navegaciones.push(new URL(destino, ubicacion.href).pathname.split('/').pop()
    + new URL(destino, ubicacion.href).search);
  // El Proxy envuelve un objeto vacío: en jsdom replace/assign no se pueden redefinir sobre location.
  const location = new Proxy({}, {
    get(_, propiedad) {
      if (propiedad === 'replace' || propiedad === 'assign') return registrar;
      if (propiedad === 'reload') return () => navegaciones.push('recargar');
      const valor = Reflect.get(ubicacion, propiedad);
      return typeof valor === 'function' ? valor.bind(ubicacion) : valor;
    },
    set(_, propiedad, valor) {
      if (propiedad === 'href') { registrar(valor); return true; }
      return Reflect.set(ubicacion, propiedad, valor);
    },
    has(_, propiedad) { return propiedad in ubicacion; }
  });

  // Ubicación del navegador (para «Cerca de mí» y el registro de locales).
  Object.defineProperty(window.navigator, 'geolocation', {
    configurable: true,
    value: {
      getCurrentPosition(exito, fallo) {
        if (geolocalizacion) exito({ coords: geolocalizacion });
        else fallo?.({ code: 1, message: 'Permiso denegado' });
      }
    }
  });

  for (const nombre of GLOBALES) {
    const valor = nombre === 'location' ? location : window[nombre];
    Object.defineProperty(globalThis, nombre, {
      configurable: true, writable: true,
      value: typeof valor === 'function' && !/^[A-Z]/.test(nombre) ? valor.bind(window) : valor
    });
  }
  globalThis.alert = texto => alertas.push({ nativo: 'alert', text: texto });
  globalThis.confirm = () => confirmar;
  globalThis.scrollTo = () => {};

  const pagina = {
    dom, window, document: window.document, llamadas, alertas, navegaciones, errores, servidor,
    confirmar,
    /** Respuestas del SweetAlert simulado: true acepta, false cancela. */
    $: selector => window.document.querySelector(selector),
    $$: selector => [...window.document.querySelectorAll(selector)],
    texto: () => window.document.body.textContent.replace(/\s+/g, ' ').trim(),
    ultimaAlerta: () => alertas.at(-1),
    llamadasA: (metodo, ruta) => llamadas.filter(l => l.metodo === metodo && l.ruta === ruta)
  };

  globalThis.Swal = {
    fire: async opciones => {
      alertas.push(opciones);
      return { isConfirmed: typeof pagina.confirmar === 'function' ? pagina.confirmar(opciones) : pagina.confirmar };
    },
    close: () => {}
  };

  globalThis.fetch = async (entrada, opciones = {}) => {
    const url = new URL(String(entrada), ubicacion.href);
    const metodo = (opciones.method || 'GET').toUpperCase();
    let cuerpo = opciones.body;
    if (typeof cuerpo === 'string') { try { cuerpo = JSON.parse(cuerpo); } catch { /* texto */ } }
    const llamada = { metodo, ruta: url.pathname, buscar: url.search, cuerpo, opciones };
    llamadas.push(llamada);

    // Las vistas del portal piden su HTML para cambiar de pantalla sin recargar.
    if (metodo === 'GET' && url.pathname.startsWith('/app/') && url.pathname.endsWith('.html')) {
      const archivo = url.pathname.slice('/app/'.length);
      if (pagina.servidor[`GET ${url.pathname}`] === undefined) {
        try { return crearRespuesta(200, readFileSync(CARPETA_APP + archivo, 'utf8'), true); }
        catch { return crearRespuesta(404, '', true); }
      }
    }

    let definida = pagina.servidor[`${metodo} ${url.pathname}${url.search}`];
    if (definida === undefined) definida = pagina.servidor[`${metodo} ${url.pathname}`];
    if (definida === undefined) return crearRespuesta(404, { message: 'Not Found' });
    if (typeof definida === 'function') definida = await definida(cuerpo, llamada);
    if (definida instanceof Error) throw definida;
    if (definida && typeof definida === 'object' && 'status' in definida && Object.keys(definida).every(k => k === 'status' || k === 'body'))
      return crearRespuesta(definida.status, definida.body);
    return crearRespuesta(200, definida);
  };

  return pagina;
}

function crearRespuesta(status, body, esTexto = false) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => {
      if (body === undefined || esTexto) throw new SyntaxError('Sin JSON');
      return structuredClone(body);
    },
    text: async () => (esTexto ? body : JSON.stringify(body ?? ''))
  };
}

/** Espera a que terminen las llamadas al servidor y las promesas pendientes. */
export async function esperar(veces = 6) {
  for (let i = 0; i < veces; i++) await new Promise(resolver => setTimeout(resolver, 0));
}

/** Escribe valores en un formulario (por name) y lo envía como lo haría la persona. */
export function llenarYEnviar(formulario, valores = {}) {
  for (const [nombre, valor] of Object.entries(valores)) {
    const campo = formulario.elements[nombre];
    if (campo.type === 'checkbox') campo.checked = Boolean(valor);
    else campo.value = valor;
    campo.dispatchEvent(new globalThis.window.Event('input', { bubbles: true }));
    campo.dispatchEvent(new globalThis.window.Event('change', { bubbles: true }));
  }
  formulario.requestSubmit();
}

/** Clic en un elemento (botón, enlace, etc.). */
export function clic(elemento) {
  elemento.dispatchEvent(new globalThis.window.MouseEvent('click', { bubbles: true, cancelable: true, button: 0 }));
}

/** Usuario consumidor de ejemplo, como lo devuelve GET /api/sesion. */
export const CONSUMIDOR = { id: 7, nombres: 'Valeria', apellidos: 'Quispe', email: 'demo.consumidor1@sazloop.test', tieneFoto: false };
