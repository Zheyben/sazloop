# Análisis del código: qué hace el sistema y en qué estado está

Fecha del análisis: 4 de noviembre de 2026 (actualizado el mismo día con el resultado de las pruebas del backend).
Alcance: solo se revisaron las carpetas `backend/` y `frontend/`.

## Cómo se hizo la revisión

- **Backend** (la parte del servidor, en Java con Spring Boot): se ejecutaron sus **pruebas automáticas** en la computadora del equipo, el 4/11/2026 a las 12:56, con `SPRING_PROFILES_ACTIVE=dev` y `mvnw test`.
  - Resultado: **12 de 12 pruebas pasaron** (7 archivos de prueba en `backend/src/test/`). Usaron **H2**, una base de datos temporal que vive en la memoria. **Con MySQL no se probó.**
  - Los resultados están en `backend/target/surefire-reports/`. El archivo `SesionUsuarioControllerTests` de esa carpeta es del 8 de octubre: es un resto antiguo, porque esa prueba ya no existe.
  - Las pruebas llaman directamente a los **servicios**, que son las clases que tienen la lógica del negocio. Casi no prueban las direcciones `/api/...` (los **controladores**, que reciben las peticiones de las pantallas). Solo se prueban los controladores de inicio y cierre de sesión. Por eso, "Funciona" en el backend quiere decir "la lógica funciona".
  - Una fila del backend dice "Lo ejecuté" solo si una prueba realmente la revisa. Las demás siguen como "Solo lo leí".
- **Frontend** (las pantallas: HTML y JavaScript): **sí se ejecutó**.
  1. Se revisó que todos los archivos `.js` no tengan errores de escritura (`node --check`).
  2. Se abrieron las pantallas en un navegador automático (Chromium con Playwright) y se hicieron clics, se llenaron formularios y se enviaron datos: **32 comprobaciones, 30 pasaron y 2 fallaron** (las dos fallas son errores reales, explicados en la tabla).
  3. Se usó una **API simulada**. La API es el conjunto de direcciones `/api/...` que las pantallas llaman para pedir o enviar datos. "Simulada" quiere decir que esas respuestas las preparó el revisor, copiando la forma exacta de las respuestas del backend.
  - Por eso, cuando una fila del frontend dice **"Funciona"**, significa que la pantalla hace bien su parte (muestra datos, valida, envía la petición correcta). **Todavía no se probó la conexión real entre las pantallas y el backend.**

Las rutas de archivos del backend en la tabla empiezan en `backend/src/main/java/com/sazloop/`. Las del frontend empiezan en `frontend/`.

## Tabla

| Qué hace (en palabras simples) | Archivo y función donde está | Estado | Cómo lo comprobaste |
|---|---|---|---|
| **[Backend] Arrancar el servidor** con la base de datos | `SazloopApplication.java` → `main()`; configuración en `resources/application*.properties` | Funciona | Lo ejecuté (prueba automática: `SazloopApplicationTests` levanta toda la aplicación con H2. Con MySQL, que es el perfil por defecto, no se probó) |
| **[Backend] Pruebas automáticas** del flujo completo (registro, documentos, compra, recojo, suspensión, sesiones) | `backend/src/test/java/com/sazloop/*Tests.java` | Funciona | Lo ejecuté (12 de 12 pasaron, el 4/11/2026) |
| **[Backend] Crear una cuenta personal** (correo único, contraseña de mínimo 8 caracteres) | `identidad/controller/RegistroUsuarioController.java` → `registrar()`; `identidad/service/RegistroUsuarioService.java` → `registrar()` | Funciona | Lo ejecuté (prueba automática: `ResolucionYSesionesTests` y `DatosDemoH2Tests`. El rechazo de un correo repetido no se prueba) |
| **[Backend] Guardar las contraseñas protegidas** (no se guardan tal cual; se transforman con un método seguro llamado PBKDF2) | `identidad/service/CodificadorContrasena.java` → `codificar()`, `coincide()` | Funciona | Lo ejecuté (prueba automática: se usa en el cambio de contraseña y en los inicios de sesión) |
| **[Backend] Iniciar, consultar y cerrar sesión** de una persona | `identidad/controller/SesionUsuarioController.java` → `iniciar()`, `actual()`, `cerrar()`; `identidad/service/SesionUsuarioService.java` | Funciona | Lo ejecuté (prueba automática: `ResolucionYSesionesTests` inicia y cierra sesión. La consulta `actual()` solo la leí) |
| **[Backend] Tener tres sesiones a la vez** (persona, trabajador del local y administrador) sin que una cierre a la otra | `shared/SesionesPorRol.java` → `abrir()`, `cerrar()` | Funciona | Lo ejecuté (prueba automática: `personaLocalYAdministradorPuedenEstarConectadosAlMismoTiempo`) |
| **[Backend] Editar perfil, cambiar contraseña y subir foto** (solo PNG o JPG de hasta 2 MB; revisa el contenido real del archivo) | `identidad/service/PerfilService.java` → `actualizar()`, `cambiarContrasena()`, `guardarFoto()`, `foto()` | Funciona | Lo ejecuté (prueba automática: `perfilActualizaDatosConFotoYProtegeContrasena`: también rechaza un correo ocupado, una contraseña actual equivocada y una foto falsa) |
| **[Backend] Registrar una empresa** (una por cuenta, RUC de 11 dígitos que no se repite) | `identidad/service/EmpresaService.java` → `registrarEmpresa()` | Funciona | Lo ejecuté (prueba automática: varias pruebas. El rechazo de un RUC repetido no se prueba) |
| **[Backend] Registrar y listar los locales** de la empresa | `identidad/service/EmpresaService.java` → `registrarEstablecimiento()`, `listarEstablecimientos()` | Funciona | Lo ejecuté (prueba automática: `PortalEmpresaEtapasTests` y otras) |
| **[Backend] Calcular indicadores de la empresa** (ventas, comisiones, ingresos, cancelaciones, no recogidas, calificación, packs entregados) | `identidad/service/IndicadoresEmpresaService.java` → `resumen()`, `calcular()` | Funciona | Lo ejecuté (prueba automática: `FlujoAfp2Tests` compara ventas, cancelaciones, no recogidas y packs entregados con los valores esperados) |
| **[Backend] Crear la cuenta del trabajador** de un local aprobado | `identidad/service/CuentaLocalService.java` → `crear()` | Funciona | Lo ejecuté (prueba automática: también rechaza crearla si el local no está aprobado. Según el código, sí deja crearla si el local está suspendido) |
| **[Backend] Iniciar sesión como trabajador** del local | `identidad/controller/CuentaLocalController.java` → `entrar()`; `CuentaLocalService.java` → `iniciar()` | Funciona | Lo ejecuté (prueba automática: el servicio `iniciar()`, incluso con el local suspendido. El controlador `entrar()` solo lo leí) |
| **[Backend] Iniciar sesión como administrador** | `afiliacion/controller/AdministradorSesionController.java` → `entrar()`; `afiliacion/service/AdministradorService.java` → `iniciar()` | Funciona | Lo ejecuté (prueba automática: `ResolucionYSesionesTests` y `DatosDemoH2Tests`) |
| **[Backend] Crear la cuenta del administrador al arrancar** | `afiliacion/service/AdministradorService.java` → `provisionarAdministrador()`; valores en `application.properties` | Incompleto | Solo lo leí (el correo y la contraseña están escritos fijos: `admin@correo.com` / `admin123`. El comentario dice que deberían venir de "variables de entorno privadas", pero no es así. Lo mismo pasa con la contraseña de MySQL en `application-mysql.properties`) |
| **[Backend] Adjuntar un documento del local** (PDF, PNG o JPG de hasta 10 MB, guardado en una carpeta privada) | `afiliacion/service/SolicitudAfiliacionService.java` → `adjuntar()`; `afiliacion/service/AlmacenDocumentoService.java` → `guardar()` | Funciona | Lo ejecuté (prueba automática: también impide adjuntar después de enviar y en una renovación solo acepta documentos vencidos o por vencer) |
| **[Backend] Enviar la solicitud de afiliación a revisión** | `afiliacion/service/SolicitudAfiliacionService.java` → `enviar()` | Funciona | Lo ejecuté (prueba automática: varias pruebas) |
| **[Backend] Descargar un documento privado** (solo el dueño del local o un administrador) | `SolicitudAfiliacionService.java` → `descargar()`; `RevisionAfiliacionService.java` → `descargar()`; `AlmacenDocumentoService.java` → `leer()` | Funciona | Lo ejecuté (prueba automática: el dueño lo descarga y otro dueño recibe "no encontrado". La descarga del administrador solo la leí) |
| **[Backend] Consultar la documentación de un local** (documentos vigentes, vencidos y por renovar) | `afiliacion/service/SolicitudAfiliacionService.java` → `documentacion()` | Funciona | Lo ejecuté (prueba automática: `PortalEmpresaEtapasTests` y `VigenciaYAvisosTests`) |
| **[Backend] Listar solicitudes pendientes y todos los locales** (para el administrador) | `afiliacion/service/RevisionAfiliacionService.java` → `pendientes()`, `consultar()`, `establecimientos()` | Funciona | Lo ejecuté (prueba automática: `PortalEmpresaEtapasTests` y `FlujoAfp2Tests`) |
| **[Backend] Revisar cada documento y decidir** (solo aprueba si todos los documentos están aprobados y ninguno vencido) | `afiliacion/service/RevisionAfiliacionService.java` → `resolver()`, `revisar()`, `decidir()` | Funciona | Lo ejecuté (prueba automática: aprobación, rechazo con motivo y rechazo sin observaciones bloqueado) |
| **[Backend] Avisar al dueño el resultado** de su solicitud o renovación | `afiliacion/service/RevisionAfiliacionService.java` → `notificarDecision()` | Funciona | Lo ejecuté (prueba automática: `VigenciaYAvisosTests` cuenta los avisos de resultado) |
| **[Backend] Controlar el vencimiento de documentos**: aviso 7 días antes, suspensión automática al vencer y reactivación al aprobar la renovación (se revisa cada minuto) | `afiliacion/service/VigenciaDocumentosService.java` → `revisarVigencias()`, `revisar()`, `suspender()`, `reactivar()` | Funciona | Lo ejecuté (prueba automática: `documentoVencidoSuspendeYLaRenovacionReactivaSinPerderReservasPagadas`. La repetición automática cada minuto no se prueba) |
| **[Backend] Crear un pack y publicar su oferta** (una sola oferta activa por local; el precio con descuento debe ser menor) | `ofertas/service/OfertaService.java` → `crearPack()`, `publicar()`, `crearYPublicar()` | Funciona | Lo ejecuté (prueba automática: también rechaza una segunda oferta activa, un inicio en el pasado y publicar con el local suspendido) |
| **[Backend] Listar los packs ya registrados** del local | `ofertas/service/OfertaService.java` → `listarPacks()` | Funciona | Lo ejecuté (prueba automática: `FlujoAfp2Tests`) |
| **[Backend] Buscar ofertas con filtros** (distrito, dirección, precio máximo, calificación mínima) | `ofertas/service/OfertaService.java` → `buscar()` | Funciona | Lo ejecuté (prueba automática: distrito, dirección y precio. El filtro de calificación solo lo leí. Filtra en la memoria del servidor: con muchas ofertas sería lento) |
| **[Backend] Ver la ficha de una oferta** | `ofertas/service/OfertaService.java` → `consultar()` | Funciona | Lo ejecuté (prueba automática: `FlujoAfp2Tests`) |
| **[Backend] Resumen del panel del trabajador** (stock, ganancia del día, semana y mes, token del QR) | `ofertas/service/PanelLocalService.java` → `resumen()`, `periodo()` | Funciona | Lo ejecuté (prueba automática: comprueba venta bruta, comisión y ganancia neta) |
| **[Backend] Comprar un pack a distancia** (bloquea el stock para que dos personas no compren la misma unidad; crea pago, boleta y código de recojo) | `reservas/service/CompraRecojoService.java` → `comprar()` | Funciona | Lo ejecuté (prueba automática: también impide comprar más de lo que hay. El pago y la boleta son **simulados** a propósito: no se cobra dinero real) |
| **[Backend] Pagar varios pedidos en una sola operación** (si uno falla, no se guarda ninguno) | `reservas/service/CompraRecojoService.java` → `comprarVarios()` | Funciona | Lo ejecuté (prueba automática: comprueba que un lote sin stock no deja reservas a medias) |
| **[Backend] Ver "Mis compras"** | `reservas/service/CompraRecojoService.java` → `misCompras()` | Funciona | Lo ejecuté (prueba automática: `FlujoAfp2Tests`) |
| **[Backend] Buscar un código de recojo y confirmar la entrega** | `reservas/service/CompraRecojoService.java` → `consultarCodigo()`, `recoger()` | Funciona | Lo ejecuté (prueba automática: también impide usar el mismo código dos veces) |
| **[Backend] Compra presencial con el QR del local** (Yape o tarjeta) | `reservas/service/CompraPresencialService.java` → `digital()` | Funciona | Lo ejecuté (prueba automática: rechaza un QR incorrecto) |
| **[Backend] Reserva en efectivo**: aparta las unidades 10 minutos, el trabajador confirma el cobro y, si no lo hace, se liberan | `reservas/service/CompraPresencialService.java` → `reservarEfectivo()`, `confirmarEfectivo()`, `vencerRetenciones()` | Funciona | Lo ejecuté (prueba automática: confirmación, doble confirmación bloqueada y devolución del stock al vencer) |
| **[Backend] Cancelar una compra** (devuelve el 50 % de forma simulada y devuelve el stock si la oferta sigue activa) | `reservas/service/CancelacionService.java` → `cancelar()` | Funciona | Lo ejecuté (prueba automática: reembolso del 50 %, stock devuelto y que nadie más pueda cancelar la compra) |
| **[Backend] Cerrar los pedidos no recogidos** cuando termina el horario (cada minuto) | `reservas/service/ReservaVencimientoService.java` → `vencer()` | Funciona | Lo ejecuté (prueba automática: `FlujoAfp2Tests` llama a `vencer()`. La repetición automática cada minuto no se prueba) |
| **[Backend] Calcular la comisión de Sazloop** (10 %) | `transacciones/service/ComisionService.java` → `recalcular()` | Funciona | Lo ejecuté (prueba automática: S/ 1.00 de S/ 10, S/ 0.50 tras cancelar y S/ 2.00 de S/ 20) |
| **[Backend] Calificar un recojo u omitir la reseña** (una reseña por cliente y local) | `experiencia/service/ResenaService.java` → `registrar()`, `omitir()` | Funciona | Lo ejecuté (prueba automática: también impide reseñar antes del recojo, reseñar dos veces o reseñar una compra ajena) |
| **[Backend] Listar notificaciones y marcarlas como leídas** | `experiencia/service/NotificacionService.java` → `listar()`, `leer()`, `leerTodas()`, `crear()`, `crearUnaVez()` | Funciona | Lo ejecuté (prueba automática: listar, leer todas y "crear una sola vez". Marcar una sola notificación (`leer()`) solo lo leí) |
| **[Backend] Avisos de pedidos** (recordatorio de recojo, compra cancelada, no recogida, efectivo vencido) | `reservas/service/AvisosReservaService.java` → `recordatorioRecojo()`, `cancelada()`, `noRecogida()`, `efectivoVencido()` | Funciona | Lo ejecuté (prueba automática: el recordatorio y el aviso de cancelación. Los avisos de "no recogida" y "efectivo vencido" se generan durante la prueba, pero ninguna prueba revisa su texto) |
| **[Backend] Calcular el impacto ambiental** (kilos de comida recuperada y CO₂ evitado) | `experiencia/domain/ImpactoCO2Evitado.java`; se guarda en `CompraRecojoService.recoger()` y `CompraPresencialService.confirmarEfectivo()` | Incompleto | Lo ejecuté (prueba automática: solo cuenta que se crea un registro por entrega. Según el código, ese registro queda **vacío**: kilos y CO₂ sin valor y `estimacionDisponible` siempre falso. Ninguna pantalla lo muestra) |
| **[Backend] Cargar datos de demostración** (locales, ofertas y cuentas ficticias; solo con los perfiles `dev` y `demo`) | `demo/DatosDemoH2.java` → `cargarDemostracion()` | Funciona | Lo ejecuté (prueba automática: `DatosDemoH2Tests`. Los datos fijos aquí son a propósito, porque es una demo) |
| **[Frontend] Ningún archivo JavaScript tiene errores de escritura** | Todos los `.js` de `frontend/` | Funciona | Lo ejecuté (`node --check`, 18 archivos sin errores) |
| **[Frontend] Página pública**: el menú y el botón "Iniciar sesión" abren y cierran la ventana de acceso | `index.html`; `js/acceso.js` → `cerrarModal()`, `finalizarCierre()` | Funciona | Lo ejecuté |
| **[Frontend] Entrega por delivery** (PedidosYa, Rappi, inDrive) | `index.html`, sección `#afiliados` | Incompleto | Solo lo leí (dice "PROXIMAMENTE..."; la página promete "elige la modalidad al comprar", pero no existe esa opción ni en el carrito ni en el backend) |
| **[Frontend] Registro de consumidor** | `app/acceso-consumidor.html`; `app/accesos.js` → `enviar()`, `vistaRegistro()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Inicio de sesión del consumidor** (si la clave es correcta entra; si es incorrecta muestra el error) | `app/accesos.js` → `enviar()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Aviso "Sin conexión" en el login de consumidor y de locales** | `app/accesos.js` → `enviar()` (usa la librería SweetAlert) | Funciona | Lo ejecuté (con SweetAlert cargado. Ojo: esa librería se descarga de internet, así que si la persona abre la página ya sin internet, el aviso tampoco podrá salir) |
| **[Frontend] Aviso "Sin conexión" en el login del administrador** | `app/acceso-administrador.html` + `app/accesos.js` → `enviar()` | No funciona | Lo ejecuté (esta página no carga SweetAlert. Al fallar la red aparece el error `Swal is not defined` y la persona no ve ningún mensaje) |
| **[Frontend] Espacio de locales**: cambiar entre "Trabajador" y "Empresa", e iniciar sesión como trabajador | `app/acceso-locales.html`; `app/accesos.js` → `cambiarRol()`, `enviar()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Inicio de sesión del administrador** | `app/acceso-administrador.html`; `app/accesos.js` → `enviar()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Mandar al login a quien entra sin sesión** | `app/portal.js` → `iniciarPortal()`; inicio de `trabajador.js`, `empresa.js`, `administrador.js` | Funciona | Lo ejecuté (en el portal del consumidor; en los otros tres portales solo lo leí) |
| **[Frontend] Barra del consumidor y cambio de pantalla sin recargar** | `app/portal.js` → `iniciarPortal()`, `actualizarNav()`; `app/portal-main.js` → `navegar()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Inicio del consumidor** (saludo y 3 packs en "Novedades") | `app/consumidor.js` → `montarInicio()`; `app/ofertas-portal.js` → `iniciarOfertas()`, `tarjeta()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Catálogo con filtros** (distrito, dirección, precio, calificación) | `app/catalogo.js` → `montarCatalogo()`; `app/ofertas-portal.js` → `parametros()`, `buscar()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Filtro "Cerca de mí"** | `app/catalogo.html` (casilla desactivada) | Incompleto | Solo lo leí (la casilla está desactivada y dice "Próximamente") |
| **[Frontend] Ver el detalle de un pack y añadirlo al carrito** | `app/ofertas-portal.js` → `abrirDetalle()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Guardar el carrito** en el navegador | `app/portal.js` → `leerCarrito()`, `guardarCarrito()` | Funciona | Lo ejecuté (se guarda en `sessionStorage`, una memoria temporal del navegador: se borra al cerrar la pestaña) |
| **[Frontend] Pagar el carrito** con Yape o tarjeta y confirmar la compra | `app/compras.js` → `mostrarCarrito()`, `pedidoActualizado()` | Funciona | Lo ejecuté (navegador + API simulada. El pago es simulado: acepta cualquier número que tenga el formato correcto) |
| **[Frontend] Términos y condiciones** | `app/terminos.html`; texto dentro de `app/compras.js` → `mostrarCarrito()` | Incompleto | Solo lo leí (el texto es "Lorem ipsum", un texto de relleno, y la página dice "Texto provisional") |
| **[Frontend] Historial de compras y "Cancelar compra"** con ventana de confirmación | `app/compras.js` → `mostrarHistorial()`, `cancelarCompra()`, `confirmarDialogo()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Pedir la reseña automáticamente** después de un recojo (estrellas y comentario) | `app/resenas-portal.js` → `avisarResenasPendientes()`, `abrirResena()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Notificaciones del consumidor** y contador de la campana | `app/notificaciones.js` → `cargar()`, `marcar()`; `app/portal.js` → `actualizarCampana()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Perfil**: editar datos, cambiar contraseña y subir foto | `app/perfil.js` → `montarPerfil()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Aviso "Sin conexión" en el perfil** cuando se llega desde el menú | `app/perfil.js` → `estado()` (dentro de `montarPerfil()`) | No funciona | Lo ejecuté (SweetAlert solo se carga si se abre `perfil.html` directamente. Al llegar por el menú, la página no se recarga, la librería no existe y el aviso da error) |
| **[Frontend] Cerrar sesión del consumidor** | `app/portal.js` → `iniciarPortal()` (botón `#salir`) | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Compra presencial escaneando el QR** (efectivo, tarjeta o Yape) | `app/qr.js` → `iniciar()`, `actualizarPago()` y el envío del formulario | Funciona | Lo ejecuté (probé efectivo y tarjeta; la opción Yape solo la leí) |
| **[Frontend] Panel del trabajador**: pack vigente, stock y ganancias | `app/trabajador.js` → `cargar()`, `pintarVigente()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Crear un pack nuevo en 3 pasos** y generar su código QR | `app/trabajador.js` → `prepararFormulario()`, `validarPaso()`, `publicar()`, `qrOferta()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Publicar de nuevo un pack ya registrado** | `app/trabajador.js` → `alternarOrigen()`, `publicar()` (opción "existente") | Parece funcionar (solo leído) | Solo lo leí |
| **[Frontend] Validar un código y confirmar la entrega** | `app/trabajador.js` → `pintarCompra()`, `confirmar()` | Funciona | Lo ejecuté (pago digital y código inexistente. La confirmación de cobro en efectivo solo la leí) |
| **[Frontend] Aviso de local suspendido** en el panel del trabajador | `app/trabajador.js` → `pintarSuspension()`, `pintarDisponibilidadPublicacion()` | Parece funcionar (solo leído) | Solo lo leí |
| **[Frontend] Registrar la empresa y su primer local** | `app/empresa.js` → envío de `#form-empresa`, `iniciarEmpresa()`, envío de `#form-local` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Adjuntar documentos y enviar la solicitud** | `app/empresa.js` → `abrirLocal()`, `seccionAfiliacion()`, `formularioDocumento()`, `botonEnviar()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Renovar documentos** vencidos o por vencer | `app/empresa.js` → `seccionVigentes()` | Parece funcionar (solo leído) | Solo lo leí |
| **[Frontend] Crear la cuenta del trabajador** desde el portal de empresa | `app/empresa.js` → `seccionCuenta()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Resumen de la empresa** con indicadores, alertas y reseñas recientes | `app/empresa.js` → `cargarResumen()`, `pintarAlertas()`, `tarjetaIndicadores()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Notificaciones de la empresa** | `app/empresa.js` → `cargarAvisos()`; `app/panel.js` → `listaAvisos()` | Funciona | Lo ejecuté (que la lista se muestre; "marcar como leída" aquí solo lo leí) |
| **[Frontend] Revisar una solicitud como administrador** (un documento vencido solo se puede rechazar, y rechazar exige escribir el motivo) | `app/administrador.js` → `cargarSolicitudes()`, `abrirSolicitud()`, `formularioResolucion()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Lista de locales para el administrador** con filtro por estado | `app/administrador.js` → `cargarEstablecimientos()`, `pintarEstablecimientos()` | Funciona | Lo ejecuté (navegador + API simulada) |
| **[Frontend] Redirigir de `negocio.html`** al portal correcto | `app/negocio.html` (script en línea) | Funciona | Lo ejecuté |

## Otros hallazgos (no son funciones, pero conviene saberlos)

- **Código que no se usa:** `frontend/app/comun.js` → `pintarNav()` no se llama desde ningún archivo.
- **Diferencia entre dos pantallas:** al validar la fecha de vencimiento de la tarjeta, `qr.js` rechaza una tarjeta que vence este mismo mes, y `compras.js` la acepta.
- **Una oferta agotada bloquea al local:** si un pack se queda sin stock, sigue contando como "oferta activa" hasta que termina su horario, y el local no puede publicar otro antes. El panel del trabajador lo explica, así que parece una decisión de diseño.
- **Contraseñas dentro del repositorio:** `application.properties` (administrador) y `application-mysql.properties` (MySQL). Es mejor sacarlas a variables de entorno, que son valores que se configuran en cada computadora y no se suben a GitHub.

## Siguiente paso: probar las pantallas con el backend real

Falta comprobar que las pantallas y el backend funcionan juntos. Para eso hay que arrancar el servidor con los datos de demostración y usar las pantallas en `http://localhost:8080`:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="dev,demo"
.\mvnw.cmd spring-boot:run
```

Las cuentas de la demo usan la contraseña `SazloopDemo2026!` (por ejemplo, `demo.consumidor1@sazloop.test`, el trabajador `demo.miraflores` y el administrador `demo.admin@sazloop.test`).

Para repetir las pruebas del backend:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd test
```
