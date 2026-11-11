# Pendientes del sistema: lo incompleto, lo no verificado y lo que falla

Fecha: 10 de noviembre de 2026. Commit revisado: `a2bdf0c`.
Este documento detalla las filas de [2-codigo.md](2-codigo.md) que **no** están en "Funciona". Son 9 puntos:

- **1 que no funciona**
- **6 incompletos**
- **2 que parecen funcionar**, pero solo se leyeron

Este documento no cambia el código: describe lo que hay. En cada punto, la parte "Qué haría falta" es una propuesta y todavía no está hecha.

Las rutas del backend empiezan en `backend/src/main/java/com/sazloop/`. Las del frontend empiezan en `frontend/`.

## Resumen

| N.º | Qué es | Estado | Cómo se comprobó | Prioridad sugerida |
|---|---|---|---|---|
| 1 | Se ve la palabra "null" suelta en tres pantallas | No funciona | Lo ejecuté | Alta (se ve en la demo) |
| 2 | No hay límite de intentos de inicio de sesión | Incompleto | Lo ejecuté | Alta (seguridad) |
| 3 | Credenciales del administrador y de MySQL escritas en el repositorio | Incompleto | Solo lo leí | Alta (seguridad) |
| 4 | Términos y condiciones con texto de relleno | Incompleto | Solo lo leí | Media (se ve en la demo) |
| 5 | Impacto ambiental (CO₂) que se guarda vacío | Incompleto | Lo ejecuté | Media (es parte del tema del proyecto) |
| 6 | Entrega por delivery anunciada pero inexistente | Incompleto | Solo lo leí | Media (la página promete algo que no existe) |
| 7 | Filtro "Cerca de mí" desactivado | Incompleto | Solo lo leí | Baja |
| 8 | Aviso de local suspendido en el panel del trabajador | Parece funcionar (solo leído) | Solo lo leí | Verificar |
| 9 | Renovar documentos vencidos o por vencer (pantalla de empresa) | Parece funcionar (solo leído) | Solo lo leí | Verificar |

---

## No funciona

### 1. Se ve la palabra "null" suelta en tres pantallas

**Estado:** No funciona. **Comprobación:** Lo ejecuté, con las pantallas conectadas al backend real. Se vio en las tres pantallas.

**Dónde aparece:**

| Pantalla | Archivo y función | Línea | Cuándo se ve |
|---|---|---|---|
| Administrador → detalle de una solicitud | `app/administrador.js` → `abrirSolicitud()` | 64–65 | Al abrir una solicitud que **no es** de renovación. El "null" sale debajo del nombre del local, encima del primer documento |
| Trabajador → "Validar código" | `app/trabajador.js` → `pintarCompra()` | 359–363 | Al consultar un código **antes** de confirmar la entrega. El "null" sale encima del código grande |
| Consumidor → "Mis notificaciones" | `app/notificaciones.js` → `cargar()` | 20–23 | Cuando **todas** las notificaciones ya están leídas. El "null" sale encima de la lista |

**Por qué pasa:**

- El proyecto tiene su propia función `el()` en `app/comun.js` (línea 39) para crear elementos de la página. Esa función **descarta** los valores `null` antes de agregarlos.
- En estas tres pantallas, en cambio, el contenido se agrega con `replaceChildren()`, que viene incluida en el navegador. Esa función no los descarta: escribe `null` como si fuera texto.
- El código usa la forma "si se cumple la condición, muestra esto; si no, `null`". Ejemplos:
  - en `administrador.js`, `s.renovacion ? banda(...) : null`;
  - en `trabajador.js`, `recienEntregada ? el(...) : null`;
  - en `notificaciones.js`, `avisos.some(a => !a.leida) ? el(...) : null`.
- Cuando la condición es falsa, `null` llega directo a `replaceChildren()` y se ve en la pantalla.

**Impacto:** no rompe ninguna función: se puede revisar, entregar y leer avisos con normalidad. Pero se ve poco profesional, sobre todo en una presentación. En la pantalla del trabajador se ve en **cada** consulta de código.

**Qué haría falta:** en esas tres líneas, cambiar `: null` por `: ''` (texto vacío). `app/empresa.js` (línea 192) ya lo hace así. Otra opción es filtrar los valores vacíos antes de llamar a `replaceChildren()`.

**Cómo comprobar la corrección:**

1. Abrir como administrador una solicitud de afiliación nueva.
2. Consultar como trabajador un código pagado sin entregar.
3. En notificaciones del consumidor, pulsar "Marcar todas como leídas" y recargar.

En ninguna de las tres debe aparecer "null".

---

## Incompletos

### 2. No hay límite de intentos de inicio de sesión

**Estado:** Incompleto. **Comprobación:** Lo ejecuté con la API real. Se enviaron 30 contraseñas equivocadas seguidas para la misma cuenta: las 30 respuestas fueron 401 ("Credenciales inválidas"), en unos 12 segundos, sin bloqueo ni espera. Inmediatamente después, la contraseña correcta entró sin problema.

**Dónde está:** los tres inicios de sesión.

- Persona (consumidor o empresa): `identidad/controller/SesionUsuarioController.java` → `iniciar()` (línea 31) y `identidad/service/SesionUsuarioService.java` → `iniciar()` (líneas 24–31).
- Trabajador: `identidad/controller/CuentaLocalController.java` → `entrar()` (línea 33) y `identidad/service/CuentaLocalService.java` → `iniciar()` (líneas 45–52).
- Administrador: `afiliacion/controller/AdministradorSesionController.java` → `entrar()` (línea 25) y `afiliacion/service/AdministradorService.java` → `iniciar()`.

En ninguno hay contador de fallos, bloqueo temporal ni espera creciente.

**Qué pasa hoy:**

- **Adivinar contraseñas:** cualquiera puede probar contraseñas sin límite contra cualquier cuenta, incluida la del administrador.
- **Saturar el servidor:** cada intento hace el cálculo de PBKDF2 con 210 000 repeticiones, que es costoso a propósito para proteger las contraseñas (unos 0,4 segundos por intento en esta computadora). Muchos intentos al mismo tiempo pueden dejar al servidor sin capacidad.
- **Saber si un correo está registrado** (solo leído):
  - Si el correo no existe, `SesionUsuarioService.iniciar()` responde de inmediato, sin hacer el cálculo (líneas 25–26).
  - Si existe, la respuesta tarda más. Midiendo el tiempo se puede saber qué correos están registrados.
  - El registro también lo revela con el mensaje "El correo ya está registrado" (`RegistroUsuarioService.java`, línea 30).

**Impacto:** en una demo local no se nota. En un servidor publicado en internet es el principal riesgo de seguridad del sistema.

**Qué haría falta:**

- Contar los intentos fallidos por cuenta y por dirección IP.
- Después de unos 5 fallos, bloquear temporalmente (por ejemplo, 15 minutos) o hacer esperar cada vez más.
- Responder "Demasiados intentos" con el código 429.
- Para que el tiempo no revele los correos: cuando el correo no existe, hacer igual el cálculo contra un hash de relleno.

**Cómo comprobarlo después:** repetir los 30 intentos fallidos. A partir del intento 6 deberían llegar respuestas 429, y la contraseña correcta debería esperar a que termine el bloqueo.

### 3. Credenciales del administrador y de MySQL escritas en el repositorio

**Estado:** Incompleto. **Comprobación:** Solo lo leí.

**Dónde está:**

- `backend/src/main/resources/application.properties`, líneas 59–60: el correo (`admin@correo.com`) y la contraseña del administrador, escritos tal cual.
- `backend/src/main/resources/application-mysql.properties`, líneas 43–44: el usuario `root` de MySQL y su contraseña, escritos tal cual. Están en GitHub desde el commit `7d9f586`.
- `afiliacion/service/AdministradorService.java` → `provisionarAdministrador()` (líneas 49–60): al arrancar, lee esos valores y crea el administrador si no existe.

**Qué pasa hoy:**

- Quien tenga acceso al repositorio conoce la contraseña del administrador y la de `root` en MySQL.
- El comentario del código (líneas 40–45) reconoce que son credenciales de demostración y que en un entorno real "vendrían de variables de entorno". Esa parte todavía no está hecha.
- La cuenta del administrador solo se crea si **no existe**. Si alguien cambia la contraseña en `application.properties`, el administrador que ya está en la base **no se actualiza**: sigue con la contraseña anterior.
- Con el perfil `demo` este paso no se ejecuta (`@Profile("!demo")`). La demo usa otra cuenta, `demo.admin@sazloop.test`.

**Impacto:**

- Si la contraseña de MySQL se usa también en otro sitio, hay que cambiarla ahí.
- Se pueden publicar en internet por accidente.
- Como el perfil por defecto es `mysql`, cualquiera que clone el proyecto queda con el mismo administrador y la misma contraseña.

**Qué haría falta:**

- Reemplazar los valores por variables, por ejemplo `sazloop.admin.password=${SAZLOOP_ADMIN_PASSWORD:}` y `spring.datasource.password=${DB_PASSWORD}`.
- Dejar un archivo de ejemplo sin valores reales, como `application-mysql.properties.example`.
- Cambiar la contraseña real de MySQL, porque la actual ya está en el historial de GitHub.

**Cómo comprobarlo después:**

- Buscar en el repositorio la palabra `password=`: no debe aparecer ningún valor real.
- Arrancar con las variables de entorno definidas y comprobar que el administrador puede entrar.

### 4. Términos y condiciones con texto de relleno

**Estado:** Incompleto. **Comprobación:** Solo lo leí.

**Dónde está:**

- `app/terminos.html`, líneas 19–23: la página dice "Texto provisional para el prototipo." y debajo tiene dos párrafos de "Lorem ipsum", un texto de relleno en latín.
- `app/compras.js` → `mostrarCarrito()`, líneas 299–301: el carrito tiene un desplegable "Términos y condiciones" cuyo contenido también es "Lorem ipsum… Aceptar para confirmar la compra."
- `app/qr.html`, línea 67: la compra presencial enlaza a `terminos.html`.

**Qué pasa hoy:**

- Para comprar hay que marcar "Acepto los términos y condiciones" (el botón no se activa sin la casilla), pero **no hay términos reales** que aceptar.
- La aceptación **no se envía al backend**: ni `CompraRequest` ni `CompraPresencialRequest` tienen un campo para ella. Así que no queda constancia de que la persona aceptó, ni de qué versión de los términos aceptó.

**Impacto:** se nota de inmediato en una presentación. Además, hay reglas de negocio que el usuario debería conocer y que no están escritas en ningún lado:

- reembolso del 50 % al cancelar, hasta el fin del horario de recojo;
- la reserva en efectivo vence a los 10 minutos;
- el pedido se cierra sin entrega si no se recoge a tiempo;
- el pago es simulado;
- el contenido del pack es sorpresa.

**Qué haría falta:**

- Redactar los términos con esas reglas.
- Usar el mismo texto en `terminos.html` y en el carrito; lo ideal es que el carrito enlace a la página en vez de copiar el texto.
- Opcional: guardar en la compra la fecha de aceptación y la versión de los términos.

**Cómo comprobarlo después:** abrir `app/terminos.html` y el desplegable del carrito, y comprobar que no queda "Lorem ipsum" ni "Texto provisional".

### 5. Impacto ambiental (CO₂) que se guarda vacío

**Estado:** Incompleto. **Comprobación:** Lo ejecuté con las pruebas automáticas, que solo verifican que se crea un registro por cada entrega. Lo demás lo leí en el código.

**Dónde está:**

- `experiencia/domain/ImpactoCO2Evitado.java`: es la tabla `impactos_co2_evitado`, con estos campos:
  - `alimentoRecuperadoKg`: kilos de comida recuperada;
  - `co2EvitadoKg`: kilos de CO₂ evitado;
  - `estimacionDisponible`: si ya hay una estimación;
  - `metodologia`: cómo se calculó.
- `reservas/service/CompraRecojoService.java` → `recoger()`, línea 143, y `reservas/service/CompraPresencialService.java` → `confirmarEfectivo()`, línea 149. En las dos se ejecuta `impactos.save(ImpactoCO2Evitado.builder().recojo(...).build())`, que crea el registro **sin llenar ningún dato**.

**Qué pasa hoy:**

- Cada entrega crea una fila con los kilos y el CO₂ **vacíos**, `estimacionDisponible = false` y la metodología vacía. El comentario de la clase lo reconoce: "Los valores pueden quedar pendientes hasta definir la fórmula oficial."
- **No hay de dónde sacar los kilos.** Los packs y sus tamaños (pequeño, mediano, grande) no tienen un campo de peso. Se buscaron las palabras "peso", "kg" y "gramos" en `ofertas/` y no hay nada.
- **Ninguna pantalla lo muestra:**
  - En el portal de empresa, "Impacto de este local" (`app/empresa.js`, línea 125) es solo el subtítulo de la cifra "Packs entregados". No muestra kilos ni CO₂.
  - `app/empresa.html` (línea 60) promete "Ventas, comisiones, ingresos **e impacto**", pero solo cuenta packs entregados.

**Impacto:** el tema del proyecto es "SalvemosLaComida", así que medir la comida que no se desperdicia es una función central. Hoy solo se cuenta cuántos packs se entregaron.

**Qué haría falta:**

1. Definir la fórmula y escribirla en `metodologia`. Por ejemplo: un peso estimado por tamaño (pequeño, mediano, grande) multiplicado por un factor de CO₂ por kilo de alimento, tomado de una fuente citada.
2. Agregar el peso estimado a cada tamaño de pack, o una tabla de pesos fija.
3. En `recoger()` y `confirmarEfectivo()`, calcular y guardar los kilos y el CO₂ con `estimacionDisponible = true`.
4. Sumarlo en `IndicadoresEmpresaService` y mostrarlo en el resumen de la empresa, y si se quiere también al consumidor.

**Cómo comprobarlo después:** entregar un pack mediano y revisar que la fila de `impactos_co2_evitado` tiene valores, y que el resumen de la empresa muestra los kilos y el CO₂ acumulados.

### 6. Entrega por delivery anunciada pero inexistente

**Estado:** Incompleto. **Comprobación:** Solo lo leí.

**Dónde está:** solo en la página pública `index.html`.

- Línea 7, la descripción de la página para buscadores: "Elige recojo en el local o entrega por delivery."
- Líneas 30 y 42: enlaces "Deliverys" en el menú.
- Línea 64: "Recojo en el local · Entrega por delivery".
- Línea 116, en el paso 3: "Recoge tu pack en el local con tu código o recíbelo mediante delivery."
- Líneas 122–134, sección `#afiliados`:
  - "puedes recibir tu pack por delivery con PedidosYa, Rappi o inDrive. **Elige la modalidad que prefieras al comprar.**";
  - debajo, "Servicios de delivery afiliados (PROXIMAMENTE...)" y los logotipos de las tres empresas.

**Qué pasa hoy:**

- En el carrito y en la compra presencial **no existe** la opción de elegir delivery. Solo hay recojo en el local.
- En el backend tampoco: ni en `CompraRequest`, ni en `Reserva`, ni en ningún otro lado hay un campo de modalidad o de dirección de entrega.
- La misma página dice a la vez "elige la modalidad al comprar" y "PROXIMAMENTE". Son mensajes que se contradicen.

**Impacto:**

- La persona espera una opción que no va a encontrar.
- Se muestran logotipos de empresas reales (PedidosYa, Rappi, inDrive) como "afiliados" sin que exista un acuerdo con ellas. Conviene presentarlo claramente como una idea futura.

**Qué haría falta, una de dos:**

- **Opción corta:** cambiar el texto para que diga que el delivery es una mejora futura. Quitar "Elige la modalidad que prefieras al comprar" y "Entrega por delivery" de la portada y de la descripción.
- **Opción completa:**
  - agregar la modalidad (recojo o delivery) y la dirección de entrega a la compra;
  - mostrar la opción en el carrito;
  - definir qué pasa con el código de recojo cuando el pack se envía.

**Cómo comprobarlo después:** que los textos de `index.html` coincidan con lo que el carrito permite hacer.

### 7. Filtro "Cerca de mí" desactivado

**Estado:** Incompleto. **Comprobación:** Solo lo leí.

**Dónde está:** `app/catalogo.html`, línea 42: `<input type="checkbox" disabled> Cerca de mí <small>Próximamente</small>`.

**Qué pasa hoy:**

- La casilla está desactivada y no se puede marcar. `app/ofertas-portal.js` → `parametros()` (líneas 85–92) solo envía distrito, dirección, precio máximo y calificación mínima.
- El backend (`ofertas/service/OfertaService.java` → `buscar()`) tampoco recibe ubicación.
- Las direcciones de los locales no tienen coordenadas (latitud y longitud), solo texto: departamento, provincia, distrito, calle y número.

**Impacto:** bajo. El filtro por distrito cubre una parte de la necesidad, y la pantalla lo marca honestamente como "Próximamente".

**Qué haría falta:**

- Guardar las coordenadas de cada local cuando se registra.
- Pedir permiso de ubicación al consumidor en el navegador.
- Enviar su posición al backend y ordenar o filtrar por distancia.

La opción más simple es ocultar la casilla hasta que exista.

**Cómo comprobarlo después:** marcar "Cerca de mí", aceptar el permiso de ubicación y ver que los packs aparecen ordenados por cercanía.

---

## Parecen funcionar (solo leídos)

Estas dos funciones están programadas y, por lo que se ve en el código, deberían funcionar. No se pudieron probar porque necesitan que un documento venza o esté por vencer.

- **Por vencer:** pasa cuando faltan 7 días o menos. Se puede preparar adjuntando un documento con esa fecha; la revisión automática lo detecta al minuto siguiente.
- **Vencido:** es más difícil de preparar. El administrador **no puede aprobar** un documento ya vencido, así que no se puede preparar desde las pantallas: habría que esperar a que venza de verdad o cambiar la fecha en la base de datos.

La lógica del backend de estos casos sí está probada por las pruebas automáticas (`VigenciaYAvisosTests`): suspensión, reactivación y avisos. Lo que falta verificar es **la pantalla**.

### 8. Aviso de local suspendido en el panel del trabajador

**Estado:** Parece funcionar (solo leído). **Comprobación:** Solo lo leí.

**Dónde está:**

- `app/trabajador.js`:
  - `suspendido()` (línea 61): devuelve verdadero si el resumen del panel trae `estadoLocal === 'SUSPENDIDO'`.
  - `pintarSuspension()` (líneas 63–68): muestra la banda roja "Local suspendido por documentos vencidos" con el motivo. Explica que se pueden validar códigos y entregar las reservas ya pagadas, pero no publicar packs.
  - `pintarVigente()` (líneas 92–102): si está suspendido, cambia "Visible para los consumidores" por "Oculto mientras el local esté suspendido" y quita el botón "Crear nuevo pack".
  - `pintarDisponibilidadPublicacion()` (líneas 261–271): muestra "No puedes publicar mientras el local esté suspendido" y desactiva los pasos del formulario.
- Backend: `ofertas/service/PanelLocalService.java` → `resumen()` (líneas 49–56). Envía `estadoLocal` y `motivoSuspension`, que toma de la última suspensión activa.

**Por qué parece funcionar:**

- El backend envía exactamente los dos datos que la pantalla lee, con los mismos nombres.
- El trabajador puede entrar aunque el local esté suspendido (`CuentaLocalService.iniciar()` lo permite, y lo comprueba una prueba automática).
- El backend también bloquea por su cuenta la publicación de un local suspendido (lo comprueba una prueba automática). Aunque la pantalla fallara, no se podría publicar.

**Qué no se sabe:** si la banda se ve bien, si el motivo aparece completo y si la pantalla de "Validar código" sigue permitiendo entregar.

**Cómo probarlo:**

1. Arrancar con `dev,demo`.
2. Abrir `http://localhost:8080/h2-console` (la consola de la base H2 del perfil `dev`) y cambiar la fecha de vencimiento del documento de un local activo a ayer. Por ejemplo, en la tabla `documentos_establecimiento`, columna `fecha_vencimiento`.
3. Esperar un minuto a que la revisión automática lo suspenda.
4. Entrar como el trabajador de ese local y revisar las tres pantallas: "Pack sorpresa", "Crear nuevo pack" y "Validar código".

### 9. Renovar documentos vencidos o por vencer (pantalla de empresa)

**Estado:** Parece funcionar (solo leído). **Comprobación:** Solo lo leí.

**Dónde está:**

- `app/empresa.js` → `seccionVigentes()` (líneas 225–254), dentro del detalle de cada local:
  - Muestra la tabla de documentos aprobados con su estado: vigente, por vencer o vencido.
  - Si hay una renovación en revisión, lo dice y muestra los documentos enviados.
  - Si la última renovación fue rechazada, muestra el motivo. Si fue aprobada, también lo indica.
  - Si no hay nada por renovar: "No hay documentos por renovar. Te avisaremos siete días antes de cada vencimiento."
  - Si el local está suspendido, pide "solo los documentos vencidos". Si no, avisa cuáles vencen pronto.
  - Muestra el formulario para adjuntar el documento renovado (`formularioDocumento()` con `renovacion = true`; ahí la fecha de vencimiento es obligatoria) y el botón "Enviar renovación a revisión".
- Backend: `afiliacion/service/SolicitudAfiliacionService.java` → `documentacion()`. Envía `documentos`, `tiposRenovables`, `tiposVencidos` y `solicitud`.

**Por qué parece funcionar:**

- Los nombres que usa la pantalla coinciden con los que envía el backend.
- La parte del backend (adjuntar solo tipos vencidos o por vencer, aprobar y reactivar) está cubierta por la prueba automática `documentoVencidoSuspendeYLaRenovacionReactivaSinPerderReservasPagadas`.
- La misma pantalla se usó hoy para una afiliación normal (adjuntar y enviar) y funcionó. La renovación reutiliza esas mismas funciones (`formularioDocumento()`, `botonEnviar()`, `tablaDocumentos()`).

**Qué no se sabe:**

- Si la sección "Renovación" aparece en el momento correcto.
- Si la lista de tipos del formulario muestra solo los renovables.
- Si después de aprobar, el local suspendido se ve otra vez "Activo" en esta pantalla.

**Cómo probarlo:**

1. Arrancar con `dev,demo`.
2. Como dueña (`duena.aurora@sazloop.test`), adjuntar en un local activo un documento con vencimiento dentro de 3 días y hacer que el administrador lo apruebe.
3. Esperar un minuto: debe aparecer como "por vencer" y el aviso debe llegar a Notificaciones.
4. Adjuntar el documento renovado, enviarlo, aprobarlo como administrador y revisar que la tabla se actualiza.
5. Para el caso de suspensión, seguir los pasos del punto 8 y luego renovar desde esta pantalla.

---

## Detalles menores encontrados en las filas que sí funcionan

No cambian el estado "Funciona" de esas filas, pero conviene corregirlos:

- **Código de recojo con guion:** el ejemplo del campo en "Validar código" (`app/trabajador.js`) dice `3F9A1C2B7D4E5A6F`, sin guion. Los códigos reales llevan guion (por ejemplo `ABBAFE9E-4869-4A`) porque salen de cortar un UUID en `CompraRecojoService`. Si se escriben sin guion, la pantalla dice "no encontrado".
- **Doble punto en el aviso de rechazo:** `afiliacion/service/RevisionAfiliacionService.java`, línea 200. Arma el texto `"Motivo: " + motivo + ". Puedes adjuntar…"`. Si el administrador escribió el motivo terminando en punto, queda "legible..".
- **Recordatorio que no se apaga:** la notificación "Recoge tu pedido" sigue sin leer después de entregar el pedido.
- **Aprobación con un solo documento:** el administrador pudo aprobar un local que solo tenía la licencia de funcionamiento. No se exige un conjunto mínimo de documentos. Hay que confirmar si es intencional.
- **Cuenta de trabajador en un local suspendido:** `CuentaLocalService.crear()` la permite, aunque el mensaje de error dice "El local debe estar aprobado". Solo lo leí.
- **Consumidor y empresa comparten la sesión:** como son la misma cuenta de persona, si en el mismo navegador alguien entra como empresa con otra cuenta, sale de la cuenta de consumidor que tenía abierta.
