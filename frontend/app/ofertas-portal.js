import { solicitar, el, dinero, tamanios, aviso, notificar, notificarError, notificarExito } from './comun.js';
import { leerCarrito, guardarCarrito } from './portal.js';

const filtrosPermitidos = ['distrito', 'direccion', 'precioMaximo', 'calificacionMinima'];

function hora(s) {
  return new Date(s).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', hour12: false });
}

function fechaBreve(s) {
  const fecha = new Date(s);
  const hoy = new Date();
  return fecha.toDateString() === hoy.toDateString()
    ? 'hoy'
    : fecha.toLocaleDateString('es-PE', { day: 'numeric', month: 'short' });
}

function horarioRecojo(o) {
  const inicio = new Date(o.inicioRecojo);
  const fin = new Date(o.finRecojo);
  const otroDia = inicio.toDateString() !== fin.toDateString();
  return `Recojo ${fechaBreve(o.inicioRecojo)} · ${hora(o.inicioRecojo)} – ${otroDia ? `${fechaBreve(o.finRecojo)} · ` : ''}${hora(o.finRecojo)}`;
}

function horarioRecojoDestacado(o) {
  const texto = horarioRecojo(o);
  const separador = texto.indexOf(' · ');
  return [el('strong', { textContent: texto.slice(0, separador) }), texto.slice(separador)];
}

function nombreDestacado(nombre) {
  const espacio = nombre.indexOf(' ');
  return espacio < 0 ? [nombre] : [nombre.slice(0, espacio), ' ', el('em', { textContent: nombre.slice(espacio + 1) })];
}

// Distancia en km con coma decimal y un decimal («1,2 km»). Menos de 100 m se muestra como «menos de 0,1 km».
function textoDistancia(km) {
  return km < 0.1 ? 'a menos de 0,1 km' : `a ${km.toLocaleString('es-PE', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} km`;
}

// Pide la ubicación al navegador. Devuelve { lat, lng } o null si el usuario no acepta o falla.
function pedirUbicacion() {
  return new Promise(resolver => {
    if (!navigator.geolocation) return resolver(null);
    navigator.geolocation.getCurrentPosition(
      posicion => resolver({ lat: posicion.coords.latitude, lng: posicion.coords.longitude }),
      () => resolver(null),
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 });
  });
}

function iconoDato(tipo) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('aria-hidden', 'true');
  svg.setAttribute('class', 'pack-icono-dato');
  const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  path.setAttribute('d', tipo === 'ubicacion'
    ? 'M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Zm-8 3a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z'
    : 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Zm0-16v6l4 2');
  svg.append(path);
  return svg;
}

function descuento(v) {
  return Math.round((1 - Number(v.precioDescuento) / Number(v.precioReferencial)) * 100);
}

export function iniciarOfertas(usuario, limite = null) {
  const formulario = document.querySelector('#filtros');
  const lista = document.querySelector('#lista');
  const detalle = document.querySelector('#detalle');
  const panelFiltros = document.querySelector('#panel-filtros');
  const alternarFiltros = document.querySelector('#alternar-filtros');
  const contadorFiltros = document.querySelector('#contador-filtros');
  let busquedaActual = 0;
  let ubicacion = null;       // { lat, lng } mientras «Cerca de mí» está activo; nunca se guarda ni se manda a otro sitio
  let mostrarDistancia = false; // las tarjetas muestran la distancia solo cuando el filtro está activo
  const valoresIniciales = new URLSearchParams(location.search);
  if (formulario) {
    for (const nombre of filtrosPermitidos) {
      if (valoresIniciales.has(nombre)) formulario.elements[nombre].value = valoresIniciales.get(nombre);
    }
    formulario.addEventListener('submit', e => {
      e.preventDefault();
      const params = parametros();
      history.replaceState(null, '', `${location.pathname}${params.size ? '?' + params : ''}`);
      panelFiltros.hidden = true;
      alternarFiltros.setAttribute('aria-expanded', 'false');
      actualizarContador(params);
      buscar(params);
    });
    // «Cerca de mí»: al marcarla el navegador pide permiso; si no se concede, se avisa y se desmarca.
    formulario.elements.cerca?.addEventListener('change', async evento => {
      const casilla = evento.target;
      if (!casilla.checked) { ubicacion = null; return; }
      casilla.disabled = true;
      ubicacion = await pedirUbicacion();
      casilla.disabled = false;
      if (!ubicacion) {
        casilla.checked = false;
        notificar({ icon: 'info', title: 'Activa la ubicación para usar este filtro' });
      }
    });
    alternarFiltros.addEventListener('click', () => {
      const abrir = panelFiltros.hidden;
      panelFiltros.hidden = !abrir;
      alternarFiltros.setAttribute('aria-expanded', String(abrir));
      if (abrir) formulario.elements.distrito.focus();
    });
    actualizarContador(parametros());
  }
  buscar(formulario ? parametros() : new URLSearchParams());

  function parametros() {
    const params = new URLSearchParams();
    for (const nombre of filtrosPermitidos) {
      const valor = formulario.elements[nombre].value.trim();
      if (valor) params.set(nombre, valor);
    }
    return params;
  }

  // Parámetros que se mandan al servidor: los filtros más la ubicación si «Cerca de mí» está activo.
  // La ubicación no se pone en la dirección de la página (URL), para no compartirla sin querer.
  function parametrosApi(params) {
    const salida = new URLSearchParams(params);
    if (ubicacion && formulario?.elements.cerca?.checked) {
      salida.set('lat', ubicacion.lat.toFixed(6));
      salida.set('lng', ubicacion.lng.toFixed(6));
    }
    return salida;
  }

  function actualizarContador(params) {
    const total = params.size + (ubicacion && formulario?.elements.cerca?.checked ? 1 : 0);
    contadorFiltros.textContent = total ? String(total) : '';
    contadorFiltros.hidden = !total;
  }

  async function buscar(params) {
    const turno = ++busquedaActual;
    lista.replaceChildren(el('p', { className: 'estado-vacio', textContent: 'Buscando packs disponibles…' }));
    const consulta = parametrosApi(params);
    mostrarDistancia = consulta.has('lat');
    try {
      const ofertas = await solicitar(`/ofertas${consulta.size ? '?' + consulta : ''}`);
      if (!lista.isConnected || turno !== busquedaActual) return;
      lista.replaceChildren(...(ofertas.length
        ? (limite ? ofertas.slice(0, limite) : ofertas).map(tarjeta)
        : [el('p', { className: 'estado-vacio', textContent: limite ? 'No tenemos productos por el momento.' : 'No hay packs con esos filtros. Prueba cambiando la búsqueda.' })]));
      aviso('');
    } catch (error) {
      if (!lista.isConnected || turno !== busquedaActual) return;
      lista.replaceChildren();
      aviso(error.message, true, 'No se pudieron cargar las ofertas');
    }
  }

  function tarjeta(o) {
    const disponibles = o.variantes.filter(v => v.stockDisponible > 0);
    const masBarata = disponibles.reduce((menor, v) => !menor || Number(v.precioDescuento) < Number(menor.precioDescuento) ? v : menor, null);
    const stock = disponibles.reduce((s, v) => s + v.stockDisponible, 0);
    return el('article', { className: 'tarjeta-pack' },
      el('div', { className: 'pack-visual' },
        el('span', { className: 'pack-visual-marca', ariaHidden: 'true', textContent: '✳' }),
        el('span', { className: 'pack-visual-stock', textContent: `${stock} uds.` }),
        el('span', { className: 'pack-visual-calificacion', textContent: `★  ${o.calificacion != null ? Number(o.calificacion).toFixed(1) : '-'}` })),
      el('div', { className: 'pack-contenido' },
        el('p', { className: 'pack-local' }, iconoDato('ubicacion'), `${o.establecimiento} · ${o.distrito}`),
        mostrarDistancia ? el('p', { className: `pack-distancia${o.distanciaKm == null ? ' sin-dato' : ''}` },
          o.distanciaKm == null ? 'Distancia no disponible' : textoDistancia(Number(o.distanciaKm))) : '',
        el('h3', {}, ...nombreDestacado(o.pack)),
        el('p', { className: 'pack-horario' }, iconoDato('reloj'), horarioRecojo(o)),
        el('div', { className: 'pack-posibles' },
          el('span', { className: 'pack-dato-etiqueta', textContent: 'PODRÍA INCLUIR' }),
          el('p', { textContent: o.posiblesProductos })),
        el('div', { className: 'pack-pie' },
          el('div', { className: 'pack-precio' },
            el('div', { className: 'pack-precio-encabezado' },
              el('span', { className: 'pack-dato-etiqueta', textContent: 'DESDE' }),
              masBarata ? el('span', { className: 'pack-descuento', textContent: `−${descuento(masBarata)} %` }) : null),
            el('div', { className: 'pack-precio-cifras' },
              el('strong', { textContent: masBarata ? dinero(masBarata.precioDescuento) : 'Sin stock' }),
              masBarata ? el('s', { textContent: dinero(masBarata.precioReferencial) }) : null)),
          el('button', { type: 'button', className: 'pack-accion', textContent: 'Ver pack', onclick: () => abrirDetalle(o.id) }))));
  }

  async function abrirDetalle(id) {
    try {
      const o = await solicitar(`/ofertas/${id}`);
      if (!detalle.isConnected) return;
      const avisoDetalle = el('p', { className: 'detalle-aviso', role: 'status', ariaLive: 'polite' });
      const total = el('strong', { className: 'detalle-total', ariaLive: 'polite' });
      const filas = o.variantes.map(v => {
        const campo = el('input', { type: 'number', min: 0, max: v.stockDisponible,
          value: 0, name: 'v' + v.id,
          disabled: v.stockDisponible === 0, ariaLabel: `Cantidad de ${tamanios[v.tamanio] || v.tamanio}` });
        const fila = el('div', { className: 'detalle-variante' },
          el('div', { className: 'detalle-variante-info' },
            el('strong', { textContent: tamanios[v.tamanio] || v.tamanio }),
            el('span', {}, el('span', { className: 'detalle-variante-precio', textContent: dinero(v.precioDescuento) }),
              ' ', el('s', { textContent: dinero(v.precioReferencial) })),
            el('small', { textContent: `${v.stockDisponible} disponibles` })),
          el('div', { className: 'detalle-cantidad' },
            el('button', { type: 'button', className: 'detalle-cantidad-boton detalle-menos', ariaLabel: `Quitar una unidad de ${tamanios[v.tamanio] || v.tamanio}`, disabled: v.stockDisponible === 0,
              onclick: () => { campo.value = Math.max(0, Math.min(v.stockDisponible, Math.trunc(Number(campo.value) || 0) - 1)); actualizarTotal(); } }),
            campo,
            el('button', { type: 'button', className: 'detalle-cantidad-boton detalle-mas', ariaLabel: `Añadir una unidad de ${tamanios[v.tamanio] || v.tamanio}`, disabled: v.stockDisponible === 0,
              onclick: () => { campo.value = Math.min(v.stockDisponible, Math.max(0, Math.trunc(Number(campo.value) || 0)) + 1); actualizarTotal(); } })));
        return fila;
      });
      const form = el('form', { id: 'formulario-detalle-pack', className: 'detalle-formulario' }, ...filas);
      function actualizarTotal() {
        const lineas = o.variantes.map(v => ({ variante: v, cantidad: Number(form.elements['v' + v.id].value) }));
        const valido = lineas.every(l => Number.isInteger(l.cantidad) && l.cantidad >= 0 && l.cantidad <= l.variante.stockDisponible);
        total.textContent = valido ? dinero(lineas.reduce((s, l) => s + l.cantidad * Number(l.variante.precioDescuento), 0)) : 'Revisa las cantidades';
        filas.forEach((fila, i) => fila.classList.toggle('seleccionada', lineas[i].cantidad > 0));
        return valido;
      }
      form.addEventListener('input', actualizarTotal);
      actualizarTotal();
      form.addEventListener('submit', e => {
        e.preventDefault();
        if (!actualizarTotal()) {
          avisoDetalle.textContent = 'Revisa las cantidades disponibles.';
          notificarError('Revisa las cantidades disponibles.', 'No se pudo añadir');
          return;
        }
        const lineas = o.variantes.map(v => ({ varianteId: v.id, cantidad: Number(form.elements['v' + v.id].value) }))
          .filter(l => l.cantidad > 0);
        if (!lineas.length || lineas.some(l => !Number.isInteger(l.cantidad))) {
          avisoDetalle.textContent = 'Elige al menos una unidad válida.';
          notificarError('Elige al menos una unidad válida.', 'No se pudo añadir');
          return;
        }
        const previos = leerCarrito(usuario.id);
        const acumuladas = new Map();
        for (const pedido of previos.filter(p => p.ofertaId === o.id)) {
          for (const linea of pedido.lineas) acumuladas.set(linea.varianteId,
            (acumuladas.get(linea.varianteId) || 0) + linea.cantidad);
        }
        for (const linea of lineas) acumuladas.set(linea.varianteId,
          (acumuladas.get(linea.varianteId) || 0) + linea.cantidad);
        if (o.variantes.some(v => (acumuladas.get(v.id) || 0) > v.stockDisponible)) {
          avisoDetalle.textContent = 'La cantidad supera las unidades disponibles.';
          notificarError('La cantidad supera las unidades disponibles.', 'No se pudo añadir');
          return;
        }
        guardarCarrito(usuario.id, [...previos, {
          clave: crypto.randomUUID(), ofertaId: o.id, lineas
        }]);
        detalle.close();
        notificarExito('Añadido al carrito', `${o.pack} ya está en tu carrito.`);
      });
      const restricciones = o.restricciones || [];
      const opcionesDieta = [
        ['vegetarianos', 'Apto para vegetarianos', 'No apto para vegetarianos'],
        ['veganos', 'Apto para veganos', 'No apto para veganos'],
        ['personas con celiaquía', 'Apto para personas con celiaquía', 'No apto para personas con celiaquía']
      ];
      const estadosDieta = opcionesDieta.map(([, positivo, negativo]) => restricciones.includes(positivo)
        ? [true, positivo] : [false, negativo]);
      const otrasRestricciones = restricciones.filter(r => !opcionesDieta.some(([, positivo, negativo]) => r === positivo || r === negativo));
      const botonAgregar = el('button', { type: 'submit', className: 'detalle-agregar', textContent: 'Añadir al carrito' });
      botonAgregar.setAttribute('form', form.id);
      detalle.replaceChildren(
        el('div', { className: 'detalle-visual' },
          el('span', { className: 'detalle-etiqueta', textContent: `★  ${o.calificacion != null ? Number(o.calificacion).toFixed(1) : '-'}` }),
          el('span', { className: 'detalle-visual-marca', ariaHidden: 'true', textContent: '✳' }),
          el('button', { type: 'button', className: 'detalle-cerrar', ariaLabel: 'Cerrar detalle', onclick: () => detalle.close() })),
        el('div', { className: 'detalle-cuerpo' },
          el('h2', { id: 'detalle-titulo' }, ...nombreDestacado(o.pack)),
          o.descripcion ? el('p', { className: 'detalle-descripcion', textContent: o.descripcion }) : null,
          el('p', { className: 'detalle-meta' }, iconoDato('ubicacion'), el('strong', { textContent: o.establecimiento }), ` · ${o.direccion}, ${o.distrito}`),
          el('p', { className: 'detalle-meta' }, iconoDato('reloj'), ...horarioRecojoDestacado(o)),
          el('section', { className: 'detalle-posibles' },
            el('span', { className: 'pack-dato-etiqueta', textContent: 'LA SORPRESA DEL DÍA · PODRÍA INCLUIR' }),
            el('p', { textContent: o.posiblesProductos })),
          o.alergenos.length ? el('section', { className: 'detalle-alergenos' },
            el('h3', { textContent: 'ALÉRGENOS' }),
            el('div', { className: 'detalle-chips' }, ...o.alergenos.map(nombre => el('span', { className: 'detalle-chip', textContent: nombre }))),
            el('p', { className: 'detalle-aviso-alergenos' },
              el('span', { className: 'detalle-info-icono', ariaHidden: 'true', textContent: 'i' }),
              'Si tienes alergia o intolerancia a alguno de estos alérgenos, este pack no es para ti.')) : null,
          estadosDieta.length || otrasRestricciones.length ? el('section', { className: 'detalle-dieta' },
            el('h3', { textContent: 'DIETA' }),
            ...estadosDieta.map(([apto, texto]) => el('p', {},
              el('span', { className: `detalle-estado ${apto ? 'apto' : 'no-apto'}`, ariaHidden: 'true', textContent: apto ? '✓' : '×' }), texto)),
            otrasRestricciones.length ? el('p', { textContent: otrasRestricciones.join(' · ') }) : null) : null,
          el('section', { className: 'detalle-seleccion' },
            el('h3', { textContent: 'Elige tu tamaño' }), form)),
        el('div', { className: 'detalle-pie' },
          el('div', { className: 'detalle-pie-total' }, el('span', { textContent: 'Total' }), total),
          el('div', { className: 'detalle-pie-accion' }, botonAgregar, avisoDetalle)));
      detalle.setAttribute('aria-labelledby', 'detalle-titulo');
      detalle.showModal();
    } catch (error) { if (detalle.isConnected) aviso(error.message, true, 'No se pudo abrir el pack'); }
  }
}
