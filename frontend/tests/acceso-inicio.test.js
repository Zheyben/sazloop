// Ventana «Iniciar sesión» de la portada (frontend/index.html + js/acceso.js).
// abrirPagina solo lee páginas de app/, así que aquí se carga index.html con un JSDOM propio.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM, VirtualConsole } from 'jsdom';
import { readFileSync } from 'node:fs';

const errores = [];
const consola = new VirtualConsole();
consola.on('jsdomError', error => errores.push(error));
const dom = new JSDOM(readFileSync(new URL('../index.html', import.meta.url), 'utf8'), {
  url: 'http://localhost:8080/index.html',
  pretendToBeVisual: true,
  virtualConsole: consola
});
const { window } = dom;
const { document } = window;

// jsdom no trae showModal ni close en <dialog>: se simulan como en navegador.js.
const prototipo = window.HTMLDialogElement.prototype;
const mostrarModal = function () { this.open = true; };
prototipo.showModal = mostrarModal;
prototipo.close = function () {
  this.open = false;
  this.dispatchEvent(new window.Event('close'));
};
globalThis.document = document;

await import('../js/acceso.js');

const modal = document.querySelector('#modal-acceso');
const enlaceEscritorio = document.querySelector('.ingreso-escritorio');
const menuMovil = document.querySelector('.menu-movil');

function clic(elemento, opciones = {}) {
  const evento = new window.MouseEvent('click', { bubbles: true, cancelable: true, ...opciones });
  elemento.dispatchEvent(evento);
  return evento;
}

function eventoAnimacion(nombre) {
  const evento = new window.Event('animationend');
  Object.defineProperty(evento, 'animationName', { value: nombre });
  return evento;
}

test('«Iniciar sesión» abre la ventana de acceso en vez de cambiar de página', () => {
  const evento = clic(enlaceEscritorio);
  assert.equal(evento.defaultPrevented, true);
  assert.equal(modal.open, true);
  assert.ok(document.body.classList.contains('modal-abierto'));
});

test('el botón × empieza la animación de cierre y la ventana se cierra al terminar', () => {
  clic(modal.querySelector('.modal-cerrar'));
  assert.ok(modal.classList.contains('cerrando'));
  assert.equal(modal.open, true);
  // Una animación distinta no cierra la ventana.
  modal.dispatchEvent(eventoAnimacion('otra-animacion'));
  assert.equal(modal.open, true);
  modal.dispatchEvent(eventoAnimacion('modal-salir'));
  assert.equal(modal.open, false);
  assert.ok(!modal.classList.contains('cerrando'));
  assert.ok(!document.body.classList.contains('modal-abierto'));
});

test('desde el menú móvil se cierra el menú y se abre la ventana', () => {
  menuMovil.open = true;
  const enlaceMovil = menuMovil.querySelector('[data-abrir-acceso]');
  clic(enlaceMovil);
  assert.equal(menuMovil.open, false);
  assert.equal(modal.open, true);
});

test('un clic dentro de la ventana no la cierra; un clic fuera del recuadro sí', () => {
  clic(modal.querySelector('h2'));
  assert.ok(!modal.classList.contains('cerrando'));
  // Clic sobre el propio <dialog> pero dentro de su recuadro (jsdom mide 0×0 en el origen).
  clic(modal, { clientX: 0, clientY: 0 });
  assert.ok(!modal.classList.contains('cerrando'));
  clic(modal, { clientX: 50, clientY: 50 });
  assert.ok(modal.classList.contains('cerrando'));
});

test('si el cierre ya está en curso, otro clic en × no hace nada más', () => {
  clic(modal.querySelector('.modal-cerrar'));
  assert.ok(modal.classList.contains('cerrando'));
  assert.equal(modal.open, true);
});

test('si la animación no termina, la ventana se cierra igual a los 380 ms', async () => {
  await new Promise(resolver => setTimeout(resolver, 450));
  assert.equal(modal.open, false);
  assert.ok(!modal.classList.contains('cerrando'));
});

test('con la ventana cerrada, la tecla Escape (cancel) no hace nada', () => {
  const evento = new window.Event('cancel', { cancelable: true });
  modal.dispatchEvent(evento);
  assert.equal(evento.defaultPrevented, true);
  assert.ok(!modal.classList.contains('cerrando'));
});

test('con la ventana abierta, la tecla Escape la cierra con animación', () => {
  clic(enlaceEscritorio);
  assert.equal(modal.open, true);
  modal.dispatchEvent(new window.Event('cancel', { cancelable: true }));
  assert.ok(modal.classList.contains('cerrando'));
  modal.dispatchEvent(eventoAnimacion('modal-salir'));
  assert.equal(modal.open, false);
});

test('en navegadores sin showModal el enlace funciona como un enlace normal', () => {
  delete prototipo.showModal;
  try {
    const evento = clic(enlaceEscritorio);
    assert.equal(evento.defaultPrevented, false);
    assert.equal(modal.open, false);
  } finally {
    prototipo.showModal = mostrarModal;
  }
});

test('no hubo errores de JavaScript en la página', () => {
  assert.deepEqual(errores.filter(e => !/Not implemented: navigation/.test(e.message)), []);
});
