# Análisis del código: qué hace el sistema y en qué estado está

Fecha del análisis: 10 de noviembre de 2026. Reemplaza la versión del 4 de noviembre.
Alcance: solo se revisaron las carpetas `backend/` y `frontend/`, en el estado del commit `a2bdf0c`.

## Cómo se hizo la revisión

Esta vez **sí se probó el sistema completo**, es decir, las pantallas conectadas al backend real.

1. **Pruebas automáticas del backend.** Se ejecutaron el 10/11/2026 con `mvnw test` y el perfil `dev`, que usa H2, una base de datos temporal en memoria.
   - Resultado: **14 de 14 pruebas pasaron**, repartidas en 7 archivos.
   - Con MySQL no se probó.
2. **Backend real en marcha.** Se arrancó el servidor con los perfiles `dev,demo` en `http://localhost:8080`. Esos perfiles cargan las empresas, locales, ofertas y cuentas de demostración.
3. **API real.** La API es el conjunto de direcciones `/api/...` que usan las pantallas.
   - Un script llamó a esas direcciones de verdad: registro, compras, entregas, afiliación, administración, etc.
   - Resultado: **85 de 85 comprobaciones pasaron**. Además de los casos normales, se probaron los casos de error: sin stock, código usado dos veces, compra ajena, archivo falso, etc.
4. **Pantallas con el backend real.** Se usaron las pantallas en un navegador: clics, formularios y pagos simulados, con los cuatro roles (consumidor, trabajador, empresa y administrador).
   - Para probar el aviso "Sin conexión" se simuló una caída de red dentro de la página.
   - Algunas pantallas no se volvieron a usar hoy. En esas filas se indica si la prueba es del 4/11 (en ese momento se usó una API simulada).
5. **Revisión de escritura del JavaScript.** Se pasó `node --check` por los 18 archivos `.js` y ninguno tiene errores.

Qué significa cada columna "Cómo lo comprobaste":

- **"Lo ejecuté (…)"**: se vio funcionar. Entre paréntesis se dice cómo:
  - "prueba automática": una de las pruebas del backend;
  - "API real": el script contra el servidor encendido;
  - "pantalla + backend real": uso en el navegador.
- **"Solo lo leí"**: solo se leyó el código, sin hacerlo funcionar.

Las rutas del backend empiezan en `backend/src/main/java/com/sazloop/`. Las del frontend empiezan en `frontend/`.

## Tabla

| Qué hace (en palabras simples) | Archivo y función donde está | Estado | Cómo lo comprobaste |
|---|---|---|---|
| **[Backend] Arrancar el servidor** con la base de datos | `SazloopApplication.java` → `main()`; configuración en `resources/application*.properties` | Funciona | Lo ejecuté (arrancó con `dev,demo` en el puerto 8080. Con MySQL, que es el perfil por defecto, no se probó) |
| **[Backend] Pruebas automáticas** (registro, documentos, compra, recojo, suspensión, sesiones) | `backend/src/test/java/com/sazloop/*Tests.java` | Funciona | Lo ejecuté (14 de 14 pasaron el 10/11/2026. Ojo: 6 de los 7 archivos no eligen un perfil. Si no se pone `SPRING_PROFILES_ACTIVE=dev`, usan MySQL y escriben en la base real) |
| **[Backend] Cargar datos de demostración** (3 empresas, 4 locales, 3 ofertas y cuentas ficticias) | `demo/DatosDemoH2.java` → `cargarDemostracion()` | Funciona | Lo ejecuté (el servidor arrancó con esos datos y todas las cuentas demo pudieron entrar. Los datos fijos son a propósito, porque es una demo) |
| **[Backend] Crear una cuenta personal** (correo único, contraseña de mínimo 8 caracteres) | `identidad/controller/RegistroUsuarioController.java` → `registrar()`; `identidad/service/RegistroUsuarioService.java` → `registrar()` | Funciona | Lo ejecuté (API real: la cuenta se crea, un correo repetido da error 409 y una contraseña corta da error 400) |
| **[Backend] Guardar las contraseñas protegidas** (con un método seguro llamado PBKDF2) | `identidad/service/CodificadorContrasena.java` → `codificar()`, `coincide()` | Funciona | Lo ejecuté (prueba automática y API real: la contraseña correcta entra y la equivocada recibe 401) |
| **[Backend] Iniciar, consultar y cerrar sesión** de una persona | `identidad/controller/SesionUsuarioController.java` → `iniciar()`, `actual()`, `cerrar()` | Funciona | Lo ejecuté (API real: entrar, consultar la sesión y cerrar. Después de cerrar, la consulta da 401) |
| **[Backend] Bloquear lo que necesita sesión** (sin sesión no se ven compras, panel ni administración) | `shared/SesionesPorRol.java`; servicios `UsuarioSesionActual`, `CuentaLocalActual` | Funciona | Lo ejecuté (API real: "Mis compras" sin sesión da 401 y una persona normal no puede ver las solicitudes del administrador) |
| **[Backend] Tener tres sesiones a la vez** (persona, trabajador y administrador) | `shared/SesionesPorRol.java` → `abrir()`, `cerrar()` | Funciona | Lo ejecuté (prueba automática y navegador. El consumidor y la empresa comparten la misma sesión de persona: si en el mismo navegador entras como empresa, sales como consumidor) |
| **[Backend] Editar perfil, cambiar contraseña y subir foto** (PNG o JPG de hasta 2 MB; revisa el contenido real del archivo) | `identidad/service/PerfilService.java` → `actualizar()`, `cambiarContrasena()`, `guardarFoto()`, `foto()` | Funciona | Lo ejecuté (API real: editar datos, rechazar un correo ocupado, rechazar una contraseña actual equivocada, cambiar la contraseña, subir y ver una foto PNG y rechazar una foto falsa) |
| **[Backend] Registrar una empresa** (una por cuenta, RUC de 11 dígitos) | `identidad/service/EmpresaService.java` → `registrarEmpresa()` | Funciona | Lo ejecuté (API real: la registra y una segunda empresa en la misma cuenta da 409. El rechazo de un RUC repetido en otra cuenta solo lo leí) |
| **[Backend] Registrar y listar los locales** de la empresa | `identidad/service/EmpresaService.java` → `registrarEstablecimiento()`, `listarEstablecimientos()` | Funciona | Lo ejecuté (API real: el local nuevo queda "pendiente de aprobación" y aparece en la lista) |
| **[Backend] Calcular indicadores de la empresa** (ventas, comisiones, ingresos, cancelaciones, calificación, packs entregados) | `identidad/service/IndicadoresEmpresaService.java` → `resumen()`, `calcular()` | Funciona | Lo ejecuté (pantalla + backend real: las cifras coincidieron con las compras hechas en la prueba. Fueron 3 ventas por S/ 42.00, con S/ 4.20 de comisión y 2 entregas) |
| **[Backend] Crear la cuenta del trabajador** de un local aprobado | `identidad/service/CuentaLocalService.java` → `crear()` | Funciona | Lo ejecuté (API real: antes de la aprobación da 409 y después se crea. Según el código, también deja crearla si el local está **suspendido**, aunque el mensaje dice "debe estar aprobado". Eso solo lo leí) |
| **[Backend] Iniciar sesión como trabajador** del local | `identidad/controller/CuentaLocalController.java` → `entrar()`; `CuentaLocalService.java` → `iniciar()` | Funciona | Lo ejecuté (API real y pantalla: la contraseña equivocada da 401 y la correcta entra al panel) |
| **[Backend] Iniciar sesión como administrador** | `afiliacion/controller/AdministradorSesionController.java` → `entrar()`; `afiliacion/service/AdministradorService.java` → `iniciar()` | Funciona | Lo ejecuté (API real y pantalla) |
| **[Backend] Crear la cuenta del administrador al arrancar** | `afiliacion/service/AdministradorService.java` → `provisionarAdministrador()`; valores en `application.properties` | Incompleto | Solo lo leí (el correo y la contraseña están escritos tal cual en `application.properties`, y la contraseña de MySQL en `application-mysql.properties`. El comentario dice que deberían venir de variables de entorno privadas. Con el perfil `demo` este paso no se usa) |
| **[Backend] Adjuntar un documento del local** (PDF, PNG o JPG de hasta 10 MB, guardado en una carpeta privada) | `afiliacion/service/SolicitudAfiliacionService.java` → `adjuntar()`; `afiliacion/service/AlmacenDocumentoService.java` → `guardar()` | Funciona | Lo ejecuté (API real: 3 PDF aceptados y un archivo que no era PDF rechazado. También en la pantalla de empresa) |
| **[Backend] Enviar la solicitud de afiliación a revisión** | `afiliacion/service/SolicitudAfiliacionService.java` → `enviar()` | Funciona | Lo ejecuté (API real y pantalla: pasa a "en revisión") |
| **[Backend] Descargar un documento privado** (solo el dueño del local o un administrador) | `SolicitudAfiliacionService.java` → `descargar()`; `RevisionAfiliacionService.java` → `descargar()`; `AlmacenDocumentoService.java` → `leer()` | Funciona | Lo ejecuté (API real: el dueño y el administrador reciben el PDF, y otra empresa recibe "no encontrado") |
| **[Backend] Consultar la documentación de un local** (documentos vigentes, vencidos y por renovar) | `afiliacion/service/SolicitudAfiliacionService.java` → `documentacion()` | Funciona | Lo ejecuté (API real y prueba automática) |
| **[Backend] Listar solicitudes pendientes y todos los locales** (para el administrador) | `afiliacion/service/RevisionAfiliacionService.java` → `pendientes()`, `consultar()`, `establecimientos()` | Funciona | Lo ejecuté (API real y pantalla) |
| **[Backend] Revisar cada documento y decidir** (rechazar exige un motivo) | `afiliacion/service/RevisionAfiliacionService.java` → `resolver()`, `revisar()`, `decidir()` | Funciona | Lo ejecuté (API real y pantalla: un rechazo sin motivo se bloquea, un rechazo con motivo se guarda y la aprobación deja el local "activo". Ojo: un local se aprobó con **un solo documento**. No se exige un conjunto mínimo, como licencia más certificado sanitario. No verificado si es intencional) |
| **[Backend] Avisar al dueño el resultado** de su solicitud | `afiliacion/service/RevisionAfiliacionService.java` → `notificarDecision()` | Funciona | Lo ejecuté (pantalla + backend real: la empresa vio el aviso del rechazo con el motivo y el de la aprobación. Detalle: el texto termina en doble punto, "legible..") |
| **[Backend] Controlar el vencimiento de documentos**: aviso 7 días antes, suspensión al vencer y reactivación al renovar (se revisa cada minuto) | `afiliacion/service/VigenciaDocumentosService.java` → `revisarVigencias()`, `revisar()`, `suspender()`, `reactivar()` | Funciona | Lo ejecuté (prueba automática. Que se repita sola cada minuto con el servidor encendido no se verificó) |
| **[Backend] Crear un pack y publicar su oferta** (una sola oferta activa por local; el precio con descuento debe ser menor) | `ofertas/service/OfertaService.java` → `crearPack()`, `publicar()`, `crearYPublicar()` | Funciona | Lo ejecuté (API real: la publica y aparece en el catálogo. Rechaza una segunda oferta activa, un horario en el pasado y un descuento mayor que el precio) |
| **[Backend] Listar los packs ya registrados** del local | `ofertas/service/OfertaService.java` → `listarPacks()` | Funciona | Lo ejecuté (API real y pantalla del trabajador) |
| **[Backend] Buscar ofertas con filtros** (distrito, dirección, precio máximo, calificación mínima) | `ofertas/service/OfertaService.java` → `buscar()` | Funciona | Lo ejecuté (API real: los cuatro filtros. Filtra en la memoria del servidor, así que con muchas ofertas sería lento) |
| **[Backend] Ver la ficha de una oferta** (incluye la calificación promedio) | `ofertas/service/OfertaService.java` → `consultar()` | Funciona | Lo ejecuté (API real: después de una reseña de 5 estrellas, la ficha muestra 5) |
| **[Backend] Resumen del panel del trabajador** (stock, ganancia del día, semana y mes, token del QR) | `ofertas/service/PanelLocalService.java` → `resumen()`, `periodo()` | Funciona | Lo ejecuté (API real y pantalla: después de las entregas, la ganancia subió de S/ 0.00 a S/ 9.90 y luego a S/ 19.80) |
| **[Backend] Comprar un pack a distancia** (bloquea el stock; crea pago, boleta y código de recojo) | `reservas/service/CompraRecojoService.java` → `comprar()` | Funciona | Lo ejecuté (API real y pantalla: devuelve boleta y código, y pedir más de lo que hay da 409. El pago y la boleta son **simulados** a propósito) |
| **[Backend] Pagar varios pedidos en una sola operación** (si uno falla, no se guarda ninguno) | `reservas/service/CompraRecojoService.java` → `comprarVarios()` | Funciona | Lo ejecuté (API real: dos pedidos de locales distintos se pagan juntos. Si uno no tiene stock, falla todo y el stock del otro no cambia) |
| **[Backend] Ver "Mis compras"** | `reservas/service/CompraRecojoService.java` → `misCompras()` | Funciona | Lo ejecuté (API real y pantalla) |
| **[Backend] Buscar un código de recojo y confirmar la entrega** | `reservas/service/CompraRecojoService.java` → `consultarCodigo()`, `recoger()` | Funciona | Lo ejecuté (API real y pantalla: un código inexistente o de otro local da "no encontrado" y usar el mismo código dos veces da 409) |
| **[Backend] Compra presencial con el QR del local** (Yape o tarjeta) | `reservas/service/CompraPresencialService.java` → `digital()` | Funciona | Lo ejecuté (API real: con el token correcto se compra y con un token falso da 409. El token del QR es fijo, así que funciona aunque la persona no esté en el local) |
| **[Backend] Reserva en efectivo**: aparta las unidades 10 minutos y el trabajador confirma el cobro | `reservas/service/CompraPresencialService.java` → `reservarEfectivo()`, `confirmarEfectivo()` | Funciona | Lo ejecuté (API real y pantalla: reserva desde la página del QR, el trabajador confirma, el stock baja y confirmar dos veces da 409) |
| **[Backend] Liberar sola una reserva en efectivo** que nadie confirmó en 10 minutos (se revisa cada minuto) | `reservas/service/CompraPresencialService.java` → `vencerRetenciones()` | Funciona | Lo ejecuté (backend real: se apartaron 2 unidades a la 01:43 y no se confirmaron. A la 01:54 el stock había vuelto de 4 a 6, el pedido estaba "EXPIRADA" y el cliente tenía el aviso "Reserva en efectivo vencida") |
| **[Backend] Cancelar una compra** (devuelve el 50 % de forma simulada y devuelve el stock) | `reservas/service/CancelacionService.java` → `cancelar()` | Funciona | Lo ejecuté (API real: compra de S/ 18.00 con S/ 9.00 de reembolso, y otra persona no puede cancelarla) |
| **[Backend] Cerrar los pedidos no recogidos** cuando termina el horario (cada minuto) | `reservas/service/ReservaVencimientoService.java` → `vencer()` | Funciona | Lo ejecuté (prueba automática. Con el servidor encendido no se esperó a que terminara un horario de recojo) |
| **[Backend] Calcular la comisión de Sazloop** (10 %) | `transacciones/service/ComisionService.java` → `recalcular()` | Funciona | Lo ejecuté (prueba automática y API real: venta de S/ 28.00 con comisión de S/ 2.80) |
| **[Backend] Calificar un recojo u omitir la reseña** (una reseña por cliente y local) | `experiencia/service/ResenaService.java` → `registrar()`, `omitir()` | Funciona | Lo ejecuté (API real y pantalla: no deja reseñar antes del recojo ni dos veces. "Omitir" solo lo probó la prueba automática) |
| **[Backend] Listar notificaciones y marcarlas como leídas** | `experiencia/service/NotificacionService.java` → `listar()`, `leer()`, `leerTodas()` | Funciona | Lo ejecuté (API real y pantalla: marcar una y marcar todas) |
| **[Backend] Avisos de pedidos** (recordatorio de recojo, cancelación, no recogida, efectivo vencido) | `reservas/service/AvisosReservaService.java` → `recordatorioRecojo()`, `cancelada()`, `noRecogida()`, `efectivoVencido()` | Funciona | Lo ejecuté (el recordatorio lo vi en pantalla, el aviso de efectivo vencido llegó con el backend real y la cancelación la revisa la prueba automática. El aviso de "no recogida" no se revisó. Detalle: el recordatorio "Recoge tu pedido" sigue sin leer después de la entrega) |
| **[Backend] Calcular el impacto ambiental** (kilos de comida y CO₂ evitado) | `experiencia/domain/ImpactoCO2Evitado.java`; se guarda en `CompraRecojoService.recoger()` y `CompraPresencialService.confirmarEfectivo()` | Incompleto | Lo ejecuté (prueba automática: solo comprueba que se crea un registro por entrega. Según el código, el registro queda **vacío**: kilos y CO₂ sin valor y `estimacionDisponible` siempre falso. Ninguna pantalla lo muestra) |
| **[Backend] Limitar los intentos de inicio de sesión** | No existe en ningún archivo | Incompleto | Lo ejecuté (API real: 30 contraseñas equivocadas seguidas recibieron 30 veces 401, sin bloqueo ni espera, y después la correcta entró) |
| **[Frontend] Ningún archivo JavaScript tiene errores de escritura** | Todos los `.js` de `frontend/` | Funciona | Lo ejecuté (`node --check` en 18 archivos, sin errores) |
| **[Frontend] Las 16 páginas cargan** desde el servidor | `index.html` y `app/*.html` | Funciona | Lo ejecuté (API real: todas responden 200) |
| **[Frontend] Página pública**: el menú y el botón "Iniciar sesión" abren y cierran la ventana de acceso | `index.html`; `js/acceso.js` → `cerrarModal()`, `finalizarCierre()` | Funciona | Lo ejecuté (el 4/11. Estos archivos no cambiaron desde el 12/10, así que la prueba sigue valiendo) |
| **[Frontend] Entrega por delivery** (PedidosYa, Rappi, inDrive) | `index.html`, sección `#afiliados` | Incompleto | Solo lo leí (dice "PROXIMAMENTE...". La página promete "elige la modalidad al comprar", pero esa opción no existe ni en el carrito ni en el backend) |
| **[Frontend] Registro de consumidor** | `app/acceso-consumidor.html`; `app/accesos.js` → `enviar()`, `vistaRegistro()` | Funciona | Lo ejecuté (pantalla + backend real: muestra "Cuenta creada") |
| **[Frontend] Inicio de sesión del consumidor** | `app/accesos.js` → `enviar()` | Funciona | Lo ejecuté (pantalla + backend real: entra al inicio con el saludo "Hola, Diego") |
| **[Frontend] Aviso "Sin conexión"** en los inicios de sesión (consumidor, locales y administrador) | `app/accesos.js` → `enviar()`; `app/comun.js` → `notificarError()` | Funciona | Lo ejecuté (se simuló una caída de red en el acceso del consumidor y en el del administrador, y el aviso salió en los dos. **Ya está corregido**: el 4/11 fallaba en el acceso del administrador. Si SweetAlert no carga, ahora se usa un aviso normal del navegador) |
| **[Frontend] Espacio de locales**: cambiar entre "Trabajador" y "Empresa" e iniciar sesión | `app/acceso-locales.html`; `app/accesos.js` → `cambiarRol()`, `enviar()` | Funciona | Lo ejecuté (pantalla + backend real, con los dos roles) |
| **[Frontend] Inicio de sesión del administrador** | `app/acceso-administrador.html`; `app/accesos.js` → `enviar()` | Funciona | Lo ejecuté (pantalla + backend real) |
| **[Frontend] Mandar al login a quien entra sin sesión** | `app/portal.js` → `iniciarPortal()`; inicio de `trabajador.js`, `empresa.js`, `administrador.js` | Funciona | Lo ejecuté (pantalla + backend real en los cuatro portales: consumidor, trabajador, empresa y administrador) |
| **[Frontend] Barra del consumidor y cambio de pantalla sin recargar** | `app/portal.js` → `iniciarPortal()`, `actualizarNav()`; `app/portal-main.js` → `navegar()` | Funciona | Lo ejecuté (pantalla + backend real: de "Mis compras" a "Mi perfil" por el menú) |
| **[Frontend] Inicio del consumidor** (saludo y packs en "Novedades") | `app/consumidor.js` → `montarInicio()`; `app/ofertas-portal.js` → `iniciarOfertas()`, `tarjeta()` | Funciona | Lo ejecuté (pantalla + backend real: muestra los packs reales con su stock) |
| **[Frontend] Catálogo con filtros** (distrito, dirección, precio, calificación) | `app/catalogo.js` → `montarCatalogo()`; `app/ofertas-portal.js` → `parametros()`, `buscar()` | Funciona | Lo ejecuté (pantalla + backend real: "Ate" con calificación mínima 3.5 dejó 1 pack) |
| **[Frontend] Filtro "Cerca de mí"** | `app/catalogo.html` (casilla desactivada) | Incompleto | Solo lo leí (la casilla está desactivada y dice "Próximamente") |
| **[Frontend] Ver el detalle de un pack y añadirlo al carrito** | `app/ofertas-portal.js` → `abrirDetalle()` | Funciona | Lo ejecuté (pantalla + backend real: muestra el aviso "Añadido al carrito") |
| **[Frontend] Guardar el carrito** en el navegador | `app/portal.js` → `leerCarrito()`, `guardarCarrito()` | Funciona | Lo ejecuté (se guarda en `sessionStorage`, aparte para cada usuario, y se vacía después de pagar) |
| **[Frontend] Pagar el carrito** con Yape o tarjeta y confirmar la compra | `app/compras.js` → `mostrarCarrito()`, `pedidoActualizado()` | Funciona | Lo ejecuté (pantalla + backend real con Yape: muestra la boleta y el código de recojo. Con tarjeta solo se probó en la pantalla el 4/11 y por API hoy. El pago es simulado: acepta cualquier número con el formato correcto) |
| **[Frontend] Términos y condiciones** | `app/terminos.html`; texto dentro de `app/compras.js` → `mostrarCarrito()` | Incompleto | Solo lo leí (el texto es "Lorem ipsum", un texto de relleno, y la página dice "Texto provisional para el prototipo") |
| **[Frontend] Historial de compras y "Cancelar compra"** con ventana de confirmación | `app/compras.js` → `mostrarHistorial()`, `cancelarCompra()` | Funciona | Lo ejecuté (pantalla + backend real: la ventana muestra el reembolso del 50 % (S/ 5.50) y "Volver" no cancela. La cancelación confirmada se probó por API) |
| **[Frontend] Pedir la reseña automáticamente** después de un recojo | `app/resenas-portal.js` → `avisarResenasPendientes()`, `abrirResena()` | Funciona | Lo ejecuté (pantalla + backend real: la ventana salió sola al entrar y la reseña de 4 estrellas cambió el promedio del local) |
| **[Frontend] Notificaciones del consumidor** y contador de la campana | `app/notificaciones.js` → `cargar()`, `marcar()`; `app/portal.js` → `actualizarCampana()` | Funciona | Lo ejecuté (pantalla + backend real: "Marcar todas como leídas" deja la campana en cero. Ver también la fila del texto "null") |
| **[Frontend] Perfil**: editar datos, cambiar contraseña y subir foto | `app/perfil.js` → `montarPerfil()` | Funciona | Lo ejecuté (hoy la pantalla cargó los datos reales y guardar se probó por API. Editar desde la pantalla se probó el 4/11 con API simulada) |
| **[Frontend] Aviso "Sin conexión" en el perfil** cuando se llega desde el menú | `app/perfil.js` → `estado()`; `app/consumidor.html` (ahora carga SweetAlert) | Funciona | Lo ejecuté (se llegó al perfil por el menú y se simuló una caída de red: el aviso salió. **Ya está corregido**: el 4/11 daba error) |
| **[Frontend] Cerrar sesión del consumidor** | `app/portal.js` → `iniciarPortal()` (botón `#salir`) | Funciona | Lo ejecuté (pantalla + backend real: vuelve al acceso y "Mis compras" ya no abre) |
| **[Frontend] Compra presencial escaneando el QR** (efectivo, tarjeta o Yape) | `app/qr.js` → `iniciar()`, `actualizarPago()` y el envío del formulario | Funciona | Lo ejecuté (pantalla + backend real con efectivo: muestra el código y la hora en que vence la reserva. Tarjeta, el 4/11 con API simulada. Yape solo lo leí) |
| **[Frontend] Panel del trabajador**: pack vigente, stock y ganancias | `app/trabajador.js` → `cargar()`, `pintarVigente()` | Funciona | Lo ejecuté (pantalla + backend real) |
| **[Frontend] Crear un pack nuevo en 3 pasos** y generar su código QR | `app/trabajador.js` → `prepararFormulario()`, `validarPaso()`, `publicar()`, `qrOferta()` | Funciona | Lo ejecuté (los 3 pasos en la pantalla, con un pack ya registrado. La opción "pack nuevo" se probó en la pantalla el 4/11 y por API hoy. Si el local ya tiene un pack publicado, la pantalla lo avisa) |
| **[Frontend] Publicar de nuevo un pack ya registrado** | `app/trabajador.js` → `alternarOrigen()`, `publicar()` (opción "existente") | Funciona | Lo ejecuté (pantalla + backend real: "Dulces del Día" quedó publicado. El 4/11 solo se había leído) |
| **[Frontend] Validar un código y confirmar la entrega** (pago digital o cobro en efectivo) | `app/trabajador.js` → `pintarCompra()`, `confirmar()` | Funciona | Lo ejecuté (pantalla + backend real, con los dos casos. Detalle: el ejemplo del campo dice `3F9A1C2B7D4E5A6F`, sin guion, pero los códigos reales llevan guion (`ABBAFE9E-4869-4A`). Escrito sin guion, dice "no encontrado") |
| **[Frontend] Aviso de local suspendido** en el panel del trabajador | `app/trabajador.js` → `pintarSuspension()`, `pintarDisponibilidadPublicacion()` | Parece funcionar (solo leído) | Solo lo leí |
| **[Frontend] Registrar la empresa y su primer local** | `app/empresa.js` → envío de `#form-empresa`, `iniciarEmpresa()`, envío de `#form-local` | Funciona | Lo ejecuté (en la pantalla el 4/11 con API simulada. Hoy se probó por API real) |
| **[Frontend] Adjuntar documentos y enviar la solicitud** | `app/empresa.js` → `abrirLocal()`, `seccionAfiliacion()`, `formularioDocumento()`, `botonEnviar()` | Funciona | Lo ejecuté (pantalla + backend real: un local rechazado ve el motivo, adjunta un PDF nuevo y vuelve a enviar) |
| **[Frontend] Renovar documentos** vencidos o por vencer | `app/empresa.js` → `seccionVigentes()` | Parece funcionar (solo leído) | Solo lo leí |
| **[Frontend] Crear la cuenta del trabajador** desde el portal de empresa | `app/empresa.js` → `seccionCuenta()` | Funciona | Lo ejecuté (en la pantalla el 4/11 con API simulada. Hoy se probó por API real) |
| **[Frontend] Resumen de la empresa** con indicadores, alertas y reseñas recientes | `app/empresa.js` → `cargarResumen()`, `pintarAlertas()`, `tarjetaIndicadores()` | Funciona | Lo ejecuté (pantalla + backend real: cifras correctas y la reseña "Muy bueno" visible) |
| **[Frontend] Notificaciones de la empresa** | `app/empresa.js` → `cargarAvisos()`; `app/panel.js` → `listaAvisos()` | Funciona | Lo ejecuté (pantalla + backend real: se marcó una como leída) |
| **[Frontend] Revisar una solicitud como administrador** (rechazar exige escribir el motivo) | `app/administrador.js` → `cargarSolicitudes()`, `abrirSolicitud()`, `formularioResolucion()` | Funciona | Lo ejecuté (pantalla + backend real: sin motivo aparece "Falta el motivo" y con motivo queda "Solicitud rechazada") |
| **[Frontend] Lista de locales para el administrador** con filtro por estado | `app/administrador.js` → `cargarEstablecimientos()`, `pintarEstablecimientos()` | Funciona | Lo ejecuté (pantalla + backend real: "Activos" muestra 4 de 5) |
| **[Frontend] Se ve la palabra "null" suelta** en tres pantallas | `app/administrador.js` → `abrirSolicitud()` (línea 64); `app/trabajador.js` → `pintarCompra()` (línea 360); `app/notificaciones.js` → `cargar()` (línea 21) | No funciona | Lo ejecuté (aparece al abrir una solicitud que no es de renovación, al consultar un código antes de entregarlo y cuando todas las notificaciones ya están leídas. La causa: se pasa `null` a `replaceChildren()` del navegador, que lo escribe como texto. La función `el()` del proyecto sí filtra `null`, pero `replaceChildren()` no lo hace) |
| **[Frontend] Redirigir de `negocio.html`** al portal correcto | `app/negocio.html` (script en línea) | Funciona | Lo ejecuté (el 4/11. El archivo no cambió desde el 12/10) |

## Otros hallazgos (no son funciones, pero conviene saberlos)

- **Ya están corregidos** los dos errores del 4/11, los del aviso "Sin conexión" en el acceso del administrador y en el perfil. También se quitó la función que no se usaba, `pintarNav()`.
- **Diferencia entre dos pantallas:** al validar la fecha de vencimiento de la tarjeta, `qr.js` rechaza una tarjeta que vence este mismo mes y `compras.js` la acepta. Solo lo leí.
- **Una oferta agotada bloquea al local:** si un pack se queda sin stock, sigue contando como "oferta activa" hasta que termina su horario, y el local no puede publicar otro antes. El panel del trabajador lo explica, así que parece una decisión de diseño.
- **Contraseñas dentro del repositorio:** están en `application.properties` (administrador) y en `application-mysql.properties` (MySQL). Es mejor pasarlas a variables de entorno, que son valores que se configuran en cada computadora y no se suben a GitHub.
- **El QR del local sirve desde cualquier lugar:** su token no cambia durante la oferta. Quien lo tenga puede hacer reservas en efectivo sin estar en el local, y cada una aparta stock por 10 minutos.
- **La hora:** casi todo el backend usa la hora de la computadora donde corre. Solo el panel del trabajador y el control de documentos usan la hora de Perú. En esta prueba la computadora estaba en hora de Perú, así que no se notó. En un servidor con otra zona horaria, los horarios de recojo podrían correrse.

## Cómo repetir esta revisión

Arrancar el servidor con los datos de demostración y abrir `http://localhost:8080`:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="dev,demo"
.\mvnw.cmd spring-boot:run
```

Algunas cuentas de la demo son `demo.consumidor1@sazloop.test`, el trabajador `demo.miraflores`, la dueña `duena.aurora@sazloop.test` y el administrador `demo.admin@sazloop.test`. Su contraseña está en `backend/src/main/java/com/sazloop/demo/DatosDemoH2.java` (constante `CLAVE`).

Repetir las pruebas del backend (siempre con el perfil `dev`, para no escribir en MySQL):

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd test
```
