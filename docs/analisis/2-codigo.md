# Análisis del código: qué hace el sistema y en qué estado está

Fecha del análisis: 11 de noviembre de 2026. Reemplaza la versión del 10 de noviembre.
Alcance: solo se revisaron las carpetas `backend/` y `frontend/`, en el estado del commit `18d327f`. Esta versión incluye los cambios del día 1, registrados en [4-cambios-dia1.md](4-cambios-dia1.md).

## Qué cambió desde la versión anterior

Los 9 puntos pendientes del 10/11, descritos en [3-pendientes.md](3-pendientes.md), quedaron así:

| Punto pendiente del 10/11 | Antes | Ahora |
|---|---|---|
| La palabra "null" suelta en tres pantallas | No funciona | **Funciona**: ya no aparece en ninguna de las tres |
| Límite de intentos de inicio de sesión | Incompleto | **Funciona**: 5 fallos dan 429 y un bloqueo |
| Credenciales en el repositorio | Incompleto | **Parcial**: la contraseña de MySQL salió del repositorio; la del administrador demo sigue escrita, por decisión del equipo |
| Términos y condiciones | Incompleto | **Funciona**: texto real de 8 puntos |
| Impacto ambiental (CO₂) | Incompleto | **Funciona**: kilos y CO₂ por local y total |
| Delivery | Incompleto | **Funciona**, simulado: PedidosYa, Rappi o inDrive |
| Filtro "Cerca de mí" | Incompleto | **Funciona**: ordena por distancia |
| Aviso de local suspendido (trabajador) | Parece funcionar (solo leído) | **Funciona**: probado con un local suspendido de verdad |
| Renovar documentos (empresa) | Parece funcionar (solo leído) | **Funciona**: renovación de un documento por vencer y de uno vencido |

## Cómo se hizo la revisión

Los cambios del día 1 no se dieron por buenos solo porque lo diga [4-cambios-dia1.md](4-cambios-dia1.md): **se volvieron a probar uno por uno**.

1. **Pruebas automáticas del backend.** Se ejecutaron el 11/11/2026 con `mvnw test` y el perfil `dev`, que usa H2, una base de datos temporal en memoria. Resultado: **14 de 14 pasaron**. El día 1 no se agregaron pruebas nuevas.
2. **Backend real en marcha.** Se arrancó el servidor con los perfiles `dev,demo` en `http://localhost:8080`. Para probar el desbloqueo sin esperar 5 minutos, el bloqueo del inicio de sesión se configuró en 1 minuto **solo para esta prueba**; no se cambió ningún archivo.
3. **Mismo script del 10/11 contra la API real**, para detectar si algo dejó de funcionar: **85 de 85 comprobaciones pasaron**. Nada de lo que funcionaba se rompió.
4. **Script nuevo para los cambios del día 1.** Cubre límite de intentos, delivery, "Cerca de mí", ubicación del local, impacto, código sin guion, una sola reserva en efectivo, recordatorios y mensajes.
   - Resultado: **44 comprobaciones**. 43 pasaron a la primera.
   - La otra falló por un error del script: mandó contraseñas cortas al inicio de sesión del trabajador, que exige 8 caracteres, así que no contaban como intento. Se repitió con contraseñas válidas y pasó.
5. **Pantallas con el backend real, en un navegador**, con los cuatro roles.
   - "Cerca de mí" y "Actualizar ubicación" se probaron con una **ubicación simulada** en Miraflores, porque el navegador de prueba no tiene GPS.
   - El caso de permiso negado también se simuló.
6. **Tareas automáticas que corren cada minuto**, preparadas cambiando fechas desde la consola de H2, que es la pantalla de la base de datos del perfil `dev`:
   - un documento con fecha vencida: el local se suspendió solo;
   - un documento que vence en 3 días: apareció "por vencer";
   - una oferta con el horario ya terminado: los pedidos sin recoger se cerraron solos.
7. **Revisión de escritura del JavaScript.** Se pasó `node --check` por los 18 archivos `.js` y ninguno tiene errores.
8. **Revisión de textos de relleno.** Se buscó en `frontend/` "Lorem ipsum", "Texto provisional" y "PROXIMAMENTE": ya no aparece ninguno.

Qué significa cada columna "Cómo lo comprobaste":

- **"Lo ejecuté (…)"**: se vio funcionar. Entre paréntesis se dice cómo:
  - "prueba automática": una de las pruebas del backend;
  - "API real": un script contra el servidor encendido;
  - "pantalla + backend real": uso en el navegador.
- **"Solo lo leí"**: solo se leyó el código, sin hacerlo funcionar.
- Si una prueba es de una fecha anterior, se indica la fecha.

**No se probó con MySQL.** Todo se hizo con H2.

Las rutas del backend empiezan en `backend/src/main/java/com/sazloop/`. Las del frontend empiezan en `frontend/`.

## Tabla

| Qué hace (en palabras simples) | Archivo y función donde está | Estado | Cómo lo comprobaste |
|---|---|---|---|
| **[Backend] Arrancar el servidor** con la base de datos | `SazloopApplication.java` → `main()`; configuración en `resources/application*.properties` | Funciona | Lo ejecuté (arrancó con `dev,demo` en el puerto 8080. Con MySQL, que es el perfil por defecto, no se probó) |
| **[Backend] Pruebas automáticas** (registro, documentos, compra, recojo, suspensión, sesiones) | `backend/src/test/java/com/sazloop/*Tests.java` | Funciona | Lo ejecuté (14 de 14 pasaron el 11/11/2026. Ojo: 6 de los 7 archivos no eligen un perfil. Si no se pone `SPRING_PROFILES_ACTIVE=dev`, usan MySQL y escriben en la base real. Las funciones nuevas del día 1 no tienen pruebas automáticas) |
| **[Backend] Cargar datos de demostración** (3 empresas, 4 locales con coordenadas, 3 ofertas y cuentas ficticias) | `demo/DatosDemoH2.java` → `cargarDemostracion()` | Funciona | Lo ejecuté (el servidor arrancó con esos datos, las cuentas demo entraron y los locales trajeron su ubicación. Los datos fijos son a propósito, porque es una demo. Ojo: los datos tardan unos segundos en cargarse después de que el servidor responde) |
| **[Backend] Usar la hora de Perú en todo el sistema** | `SazloopApplication.java` (bloque `static` con `TimeZone.setDefault`); `application.properties` → `spring.jackson.time-zone`, `hibernate.jdbc.time_zone` | Parece funcionar (solo leído) | Solo lo leí (en esta computadora, que ya está en hora de Perú, la hora en que vence una reserva salió exactamente 10 minutos después de la hora local. Eso no prueba el caso importante, que es un servidor en otra zona horaria) |
| **[Backend] Crear una cuenta personal** (correo único, contraseña de mínimo 8 caracteres) | `identidad/controller/RegistroUsuarioController.java` → `registrar()`; `identidad/service/RegistroUsuarioService.java` → `registrar()` | Funciona | Lo ejecuté (API real: la cuenta se crea, un correo repetido da 409 y una contraseña corta da 400) |
| **[Backend] Guardar las contraseñas protegidas** (con un método seguro llamado PBKDF2) | `identidad/service/CodificadorContrasena.java` → `codificar()`, `coincide()`, `rellenar()` | Funciona | Lo ejecuté (prueba automática y API real) |
| **[Backend] Iniciar, consultar y cerrar sesión** de una persona | `identidad/controller/SesionUsuarioController.java` → `iniciar()`, `actual()`, `cerrar()` | Funciona | Lo ejecuté (API real: entrar, consultar y cerrar. Después de cerrar, la consulta da 401) |
| **[Backend] Limitar los intentos de inicio de sesión** (5 fallos seguidos bloquean la cuenta 5 minutos y la respuesta es 429) | `shared/LimiteIntentosLogin.java` → `exigirDisponible()`, `registrarFallo()`, `registrarExito()`; se usa en `SesionUsuarioService`, `CuentaLocalService` y `AdministradorService` | Funciona | Lo ejecuté (API real, en los tres accesos: 5 veces 401 y la sexta 429. Durante el bloqueo, la contraseña correcta también recibe 429. Un correo que no existe también se bloquea. A los 62 segundos, con el bloqueo configurado en 1 minuto, la contraseña correcta volvió a entrar en los tres. Ojo: el bloqueo es por cuenta, no por dirección IP, así que cualquiera que sepa un correo puede bloquear esa cuenta 5 minutos) |
| **[Backend] Tardar lo mismo aunque la cuenta no exista** (para que no se pueda adivinar qué correos están registrados) | `identidad/service/CodificadorContrasena.java` → `rellenar()` | Funciona | Lo ejecuté (API real: en promedio, 405 ms con una cuenta que existe y 401 ms con una que no existe. Ojo: el registro todavía avisa "El correo ya está registrado") |
| **[Backend] Bloquear lo que necesita sesión** (sin sesión no se ven compras, panel ni administración) | `shared/SesionesPorRol.java`; `UsuarioSesionActual`, `CuentaLocalActual` | Funciona | Lo ejecuté (API real: "Mis compras" sin sesión da 401, y una persona normal no puede ver las solicitudes del administrador) |
| **[Backend] Tener tres sesiones a la vez** (persona, trabajador y administrador) | `shared/SesionesPorRol.java` → `abrir()`, `cerrar()` | Funciona | Lo ejecuté (prueba automática y navegador. El consumidor y la empresa comparten la misma sesión de persona) |
| **[Backend] Editar perfil, cambiar contraseña y subir foto** | `identidad/service/PerfilService.java` → `actualizar()`, `cambiarContrasena()`, `guardarFoto()`, `foto()` | Funciona | Lo ejecuté (API real: los 7 casos del 10/11 volvieron a pasar) |
| **[Backend] Registrar una empresa** (una por cuenta, RUC de 11 dígitos) | `identidad/service/EmpresaService.java` → `registrarEmpresa()` | Funciona | Lo ejecuté (API real. El rechazo de un RUC repetido en otra cuenta solo lo leí) |
| **[Backend] Registrar y listar los locales**, con su ubicación opcional | `identidad/service/EmpresaService.java` → `registrarEstablecimiento()`, `listarEstablecimientos()` | Funciona | Lo ejecuté (API real: un local con coordenadas guarda su latitud y longitud, uno sin coordenadas también se registra, y una latitud de 120 da 400) |
| **[Backend] Guardar o cambiar la ubicación de un local** (servicio nuevo: `PUT /api/empresas/actual/establecimientos/{id}/ubicacion`) | `identidad/controller/EmpresaController.java`; `identidad/service/EmpresaService.java` → `actualizarUbicacion()`; `identidad/dto/UbicacionRequest.java` | Funciona | Lo ejecuté (API real: el dueño la cambia (200) y queda guardada, una latitud de −91 da 400 y otra empresa recibe 404) |
| **[Backend] Calcular indicadores de la empresa** (ventas, comisiones, ingresos, cancelaciones, calificación, packs entregados) | `identidad/service/IndicadoresEmpresaService.java` → `resumen()`, `calcular()` | Funciona | Lo ejecuté (pantalla + backend real) |
| **[Backend] Calcular el impacto ambiental** de cada entrega (kilos = peso por tamaño × unidades, con 0,5 / 1,0 / 1,5 kg; CO₂ = kilos × 2,5) | `experiencia/service/ImpactoService.java` → `registrar()`; se usa en `CompraRecojoService.recoger()` y `CompraPresencialService.confirmarEfectivo()`; la suma está en `IndicadoresEmpresaService` | Funciona | Lo ejecuté (API real: 2 packs medianos entregados dieron 2 kg de comida y 5 kg de CO₂ en el local y en el total. Otra empresa con 4 entregas mostró 4 kg y 10 kg. Ojo: los pesos son una suposición del proyecto y el factor 2,5 viene de FAO (2013)) |
| **[Backend] Crear la cuenta del trabajador** de un local aprobado | `identidad/service/CuentaLocalService.java` → `crear()` | Funciona | Lo ejecuté (API real: antes de la aprobación da 409 con el mensaje nuevo "El local debe haber sido aprobado". Sigue permitiendo crearla con el local suspendido, a propósito; eso solo lo leí) |
| **[Backend] Iniciar sesión como trabajador** | `identidad/controller/CuentaLocalController.java` → `entrar()`; `CuentaLocalService.java` → `iniciar()` | Funciona | Lo ejecuté (API real y pantalla. También entra con el local suspendido, como corresponde) |
| **[Backend] Iniciar sesión como administrador** | `afiliacion/controller/AdministradorSesionController.java` → `entrar()`; `afiliacion/service/AdministradorService.java` → `iniciar()` | Funciona | Lo ejecuté (API real y pantalla) |
| **[Backend] Crear la cuenta del administrador al arrancar** | `afiliacion/service/AdministradorService.java` → `provisionarAdministrador()`; valores en `application.properties` | Funciona | Lo ejecuté (prueba automática con el perfil `dev`. El correo y la contraseña del administrador siguen escritos en `application.properties`. El comentario del código explica que son credenciales de demostración y que así lo decidió el equipo) |
| **[Backend] Leer la contraseña de MySQL de un archivo local que no se sube a GitHub** | `resources/application-mysql.properties` (valor de ejemplo y `spring.config.import=optional:file:./application-local.properties`); `backend/application-local.properties.example`; `.gitignore` | Parece funcionar (solo leído) | Solo lo leí (el repositorio solo tiene valores de ejemplo, el archivo local está en `.gitignore` y existe el `.example`. No se arrancó con MySQL. Ojo: la contraseña anterior **sigue en el historial de git**) |
| **[Backend] Adjuntar un documento del local** (PDF, PNG o JPG de hasta 10 MB) | `afiliacion/service/SolicitudAfiliacionService.java` → `adjuntar()`; `afiliacion/service/AlmacenDocumentoService.java` → `guardar()` | Funciona | Lo ejecuté (API real y pantalla. En una renovación, un tipo de documento que no está vencido ni por vencer da 409) |
| **[Backend] Enviar la solicitud de afiliación o de renovación a revisión** | `afiliacion/service/SolicitudAfiliacionService.java` → `enviar()` | Funciona | Lo ejecuté (API real y pantalla) |
| **[Backend] Descargar un documento privado** (solo el dueño o un administrador) | `SolicitudAfiliacionService.java` → `descargar()`; `RevisionAfiliacionService.java` → `descargar()`; `AlmacenDocumentoService.java` → `leer()` | Funciona | Lo ejecuté (API real) |
| **[Backend] Consultar la documentación de un local** (vigentes, por vencer, vencidos y renovables) | `afiliacion/service/SolicitudAfiliacionService.java` → `documentacion()` | Funciona | Lo ejecuté (API real: un documento que vence en 3 días salió "por vencer" y un documento renovado salió "vigente") |
| **[Backend] Listar solicitudes pendientes y todos los locales** (para el administrador) | `afiliacion/service/RevisionAfiliacionService.java` → `pendientes()`, `consultar()`, `establecimientos()` | Funciona | Lo ejecuté (API real y pantalla) |
| **[Backend] Revisar cada documento y decidir** (rechazar exige un motivo) | `afiliacion/service/RevisionAfiliacionService.java` → `resolver()`, `revisar()`, `decidir()` | Funciona | Lo ejecuté (API real y pantalla. Ojo, sigue igual que el 10/11: un local se puede aprobar con **un solo documento**. No verificado si es intencional) |
| **[Backend] Avisar al dueño el resultado** de su solicitud o renovación | `afiliacion/service/RevisionAfiliacionService.java` → `notificarDecision()` | Funciona | Lo ejecuté (API real: el motivo "Documento ilegible." quedó como "…ilegible. Puedes adjuntar…", con un solo punto. Las renovaciones aprobadas también avisan, y en el local reactivado dice "vuelve a aparecer en la búsqueda") |
| **[Backend] Controlar el vencimiento de documentos**: aviso 7 días antes, suspensión al vencer y reactivación al renovar (se revisa cada minuto) | `afiliacion/service/VigenciaDocumentosService.java` → `revisarVigencias()`, `revisar()`, `suspender()`, `reactivar()` | Funciona | Lo ejecuté (backend real. Con un documento vencido, el local se suspendió solo en menos de un minuto: desapareció de la búsqueda, no pudo publicar (409) y la dueña recibió el aviso. Con un documento que vence en 3 días llegó "Documento por vencer". Al aprobar la renovación, el local suspendido volvió a "activo" y a la búsqueda) |
| **[Backend] Crear un pack y publicar su oferta** (una sola oferta activa por local; el precio con descuento debe ser menor) | `ofertas/service/OfertaService.java` → `crearPack()`, `publicar()`, `crearYPublicar()` | Funciona | Lo ejecuté (API real: los mismos casos del 10/11. Con el local suspendido da 409) |
| **[Backend] Listar los packs ya registrados** del local | `ofertas/service/OfertaService.java` → `listarPacks()` | Funciona | Lo ejecuté (API real) |
| **[Backend] Buscar ofertas con filtros** (distrito, dirección, precio máximo, calificación mínima), ahora filtrando en la base de datos | `ofertas/service/OfertaService.java` → `buscar()`; `ofertas/repository/OfertaBusquedaRepository.java` | Funciona | Lo ejecuté (API real: los filtros del 10/11 dan los mismos resultados. Que ahora haga 8 consultas fijas, como dice el registro del día 1, solo lo leí. En MySQL no se probó; ahí las tildes podrían no contar al buscar) |
| **[Backend] Ordenar por cercanía** ("Cerca de mí": `GET /api/ofertas?lat=&lng=`, con distancia en línea recta) | `ofertas/service/OfertaService.java` → `buscar()`; `ofertas/controller/OfertaController.java` → `buscar()` | Funciona | Lo ejecuté (API real desde Miraflores: Aurora Miraflores a 0,0 km, Oriente Pan a 11,9 km, Sazón Urbano a 14,2 km y al final el local sin coordenadas, sin distancia. Si falta `lng` o la latitud es 100, da 400 "Ubicación inválida". Combinado con el filtro de distrito, también funciona) |
| **[Backend] Ver la ficha de una oferta** | `ofertas/service/OfertaService.java` → `consultar()` | Funciona | Lo ejecuté (API real) |
| **[Backend] Resumen del panel del trabajador** (stock, ganancias, token del QR y estado del local) | `ofertas/service/PanelLocalService.java` → `resumen()`, `periodo()` | Funciona | Lo ejecuté (API real y pantalla: con el local suspendido muestra el estado y el motivo) |
| **[Backend] Comprar un pack a distancia** (bloquea el stock; crea pago, boleta y código de recojo) | `reservas/service/CompraRecojoService.java` → `comprar()` | Funciona | Lo ejecuté (API real y pantalla. Sin modalidad, la compra es de recojo. El pago y la boleta son **simulados** a propósito) |
| **[Backend] Comprar con delivery simulado** (PedidosYa, Rappi o inDrive, con distrito, dirección y referencia; Sazloop no cobra el envío) | `reservas/service/CompraRecojoService.java` → `comprar()`, `comprarVarios()`; `reservas/domain/Reserva.java` (campos de entrega); `reservas/dto/CompraRequest.java`, `CompraMultipleRequest.java` | Funciona | Lo ejecuté (API real y pantalla: sin dirección da 400 "Indica la dirección de entrega" y sin empresa 400 "Elige la empresa de delivery". Con todo, se crea con el total igual al precio del pack. En el lote, los dos pedidos guardaron la empresa y la dirección) |
| **[Backend] Pagar varios pedidos en una sola operación** (si uno falla, no se guarda ninguno) | `reservas/service/CompraRecojoService.java` → `comprarVarios()` | Funciona | Lo ejecuté (API real) |
| **[Backend] Ver "Mis compras"** (ahora con la modalidad y los datos de entrega) | `reservas/service/CompraRecojoService.java` → `misCompras()` | Funciona | Lo ejecuté (API real y pantalla) |
| **[Backend] Buscar un código de recojo y confirmar la entrega** (con o sin guion, en mayúsculas o minúsculas) | `reservas/service/CompraRecojoService.java` → `consultarCodigo()`, `recoger()`; `reservas/repository/CodigoRecojoRepository.java` | Funciona | Lo ejecuté (API real y pantalla: `37f2a5d36a3947`, sin guion y en minúsculas, encontró `37F2A5D3-6A39-47`, y con espacios también. Un pedido delivery "entregado al repartidor" queda "recogido" y con la reseña pendiente) |
| **[Backend] Compra presencial con el QR del local** (Yape o tarjeta) | `reservas/service/CompraPresencialService.java` → `digital()` | Funciona | Lo ejecuté (API real: pedir delivery con QR da 400 "La compra con QR no admite delivery") |
| **[Backend] Reserva en efectivo** (aparta las unidades 10 minutos; **una sola pendiente por consumidor**) | `reservas/service/CompraPresencialService.java` → `reservarEfectivo()`, `confirmarEfectivo()`; `reservas/repository/RetencionStockRepository.java` | Funciona | Lo ejecuté (API real: la primera reserva se crea y la segunda da 409 "Ya tienes una reserva en efectivo pendiente…". El trabajador confirmó el cobro escribiendo el código sin guion) |
| **[Backend] Liberar sola una reserva en efectivo** que nadie confirmó en 10 minutos | `reservas/service/CompraPresencialService.java` → `vencerRetenciones()` | Funciona | Lo ejecuté (el 10/11, con el backend real: el stock volvió, el pedido quedó "EXPIRADA" y llegó el aviso. El 11/11 no se repitió) |
| **[Backend] Cancelar una compra** (devuelve el 50 % de forma simulada; solo las compras desde la web) | `reservas/service/CancelacionService.java` → `cancelar()` | Funciona | Lo ejecuté (API real: un delivery de S/ 18.00 devolvió S/ 9.00) |
| **[Backend] Cerrar los pedidos no recogidos** cuando termina el horario (cada minuto) | `reservas/service/ReservaVencimientoService.java` → `vencer()` | Funciona | Lo ejecuté (backend real: se adelantó el fin del horario de una oferta. En menos de un minuto, un pedido de recojo y uno de delivery quedaron "EXPIRADA") |
| **[Backend] Calcular la comisión de Sazloop** (10 %) | `transacciones/service/ComisionService.java` → `recalcular()` | Funciona | Lo ejecuté (prueba automática y pantalla: venta de S/ 11.00 con S/ 1.10 de comisión) |
| **[Backend] Calificar un recojo u omitir la reseña** | `experiencia/service/ResenaService.java` → `registrar()`, `omitir()` | Funciona | Lo ejecuté (API real y pantalla) |
| **[Backend] Listar notificaciones y marcarlas como leídas** | `experiencia/service/NotificacionService.java` → `listar()`, `leer()`, `leerTodas()`, `leerPorReferencia()` | Funciona | Lo ejecuté (API real y pantalla) |
| **[Backend] Avisos de pedidos** (recordatorio, delivery, cancelación, no recogido, no entregado al repartidor, efectivo vencido) | `reservas/service/AvisosReservaService.java` → `recordatorioRecojo()`, `cancelada()`, `noRecogida()`, `efectivoVencido()`, `cerrarRecordatorio()` | Funciona | Lo ejecuté (API real: el delivery avisa "Tu pedido llegará con PedidosYa a Calle Las Flores 123, Miraflores…". El no recogido dice "Pedido no recogido…" y el delivery no entregado dice "Pedido no entregado al repartidor…". **El recordatorio queda leído** al entregar, al cancelar y al vencer el pedido) |
| **[Frontend] Ningún archivo JavaScript tiene errores de escritura** | Todos los `.js` de `frontend/` | Funciona | Lo ejecuté (`node --check` en 18 archivos el 11/11, sin errores) |
| **[Frontend] Las 16 páginas cargan** desde el servidor | `index.html` y `app/*.html` | Funciona | Lo ejecuté (API real: todas responden 200) |
| **[Frontend] Página pública**: el menú y el botón "Iniciar sesión" abren y cierran la ventana de acceso | `index.html`; `js/acceso.js` → `cerrarModal()`, `finalizarCierre()` | Funciona | Lo ejecuté (el 4/11. `js/acceso.js` no cambió. En `index.html` solo cambiaron los textos del delivery) |
| **[Frontend] Textos del delivery en la página pública** | `index.html`, sección `#afiliados` | Funciona | Solo lo leí (ya no dice "PROXIMAMENTE". Dice "Servicios de delivery disponibles (simulado)" y "El delivery es simulado, como los pagos", lo que coincide con el carrito) |
| **[Frontend] Registro de consumidor** | `app/acceso-consumidor.html`; `app/accesos.js` → `enviar()`, `vistaRegistro()` | Funciona | Lo ejecuté (el 10/11, pantalla + backend real. `accesos.js` solo cambió en el mensaje del 429) |
| **[Frontend] Inicio de sesión** de consumidor, locales y administrador | `app/accesos.js` → `enviar()`, `cambiarRol()` | Funciona | Lo ejecuté (pantalla + backend real con los cuatro roles) |
| **[Frontend] Mensaje de cuenta bloqueada** al iniciar sesión | `app/accesos.js` → `enviar()` (respuesta 429) | Funciona | Lo ejecuté (pantalla + backend real: muestra "Demasiados intentos fallidos. Espera 1 minuto e inténtalo de nuevo.", tal como lo manda el servidor) |
| **[Frontend] Aviso "Sin conexión"** en los inicios de sesión y en el perfil | `app/accesos.js` → `enviar()`; `app/comun.js` → `notificarError()`; `app/perfil.js` → `estado()` | Funciona | Lo ejecuté (el 10/11, simulando una caída de red. Esa parte no cambió el día 1) |
| **[Frontend] Mandar al login a quien entra sin sesión** | `app/portal.js` → `iniciarPortal()`; inicio de `trabajador.js`, `empresa.js`, `administrador.js` | Funciona | Lo ejecuté (el 10/11 en los cuatro portales. El 11/11 se vio otra vez en el administrador) |
| **[Frontend] Barra del consumidor y cambio de pantalla sin recargar** | `app/portal.js`; `app/portal-main.js` → `navegar()` | Funciona | Lo ejecuté (pantalla + backend real) |
| **[Frontend] Inicio del consumidor** (saludo y "Novedades") | `app/consumidor.js` → `montarInicio()`; `app/ofertas-portal.js` → `iniciarOfertas()`, `tarjeta()` | Funciona | Lo ejecuté (pantalla + backend real) |
| **[Frontend] Catálogo con filtros** (distrito, dirección, precio, calificación) | `app/catalogo.js` → `montarCatalogo()`; `app/ofertas-portal.js` → `parametros()`, `buscar()` | Funciona | Lo ejecuté (el 10/11 en la pantalla. El 11/11, los mismos filtros por API real) |
| **[Frontend] Filtro "Cerca de mí"** (pide permiso de ubicación y muestra la distancia en cada tarjeta) | `app/catalogo.html`; `app/ofertas-portal.js` | Funciona | Lo ejecuté (pantalla + backend real, con ubicación simulada: ordena "a menos de 0,1 km", "a 11,9 km", "a 14,2 km" y al final "Distancia no disponible". La ubicación no aparece en la dirección de la página. Con el permiso negado sale "Activa la ubicación para usar este filtro" y la casilla se desmarca. No se probó con un GPS real) |
| **[Frontend] Ver el detalle de un pack y añadirlo al carrito** | `app/ofertas-portal.js` → `abrirDetalle()` | Funciona | Lo ejecuté (pantalla + backend real) |
| **[Frontend] Guardar el carrito** en el navegador | `app/portal.js` → `leerCarrito()`, `guardarCarrito()` | Funciona | Lo ejecuté (pantalla + backend real) |
| **[Frontend] Pagar el carrito** con Yape o tarjeta, con recojo o con delivery | `app/compras.js` → `mostrarCarrito()`, `pedidoActualizado()` | Funciona | Lo ejecuté (pantalla + backend real con Yape y delivery por inDrive. Con delivery elegido y sin dirección, el botón de pagar queda bloqueado; al llenar distrito y dirección se activa. La confirmación muestra "Modalidad: Delivery con inDrive – Av. Javier Prado 500, San Isidro") |
| **[Frontend] Validar la fecha de la tarjeta** (acepta el mes actual y rechaza el anterior) | `app/comun.js` → `tarjetaVigente()`; se usa en `app/compras.js` y `app/qr.js` | Funciona | Lo ejecuté (la función, en el navegador: el mes actual es válido y el anterior no. Las dos pantallas usan la misma función. Pagar con tarjeta en `qr.html` no se probó) |
| **[Frontend] Términos y condiciones** (8 puntos: pagos simulados, pack sorpresa, cancelación, no recojo, efectivo, delivery, alérgenos, tarjeta) | `app/terminos.html`; resumen y enlace en `app/compras.js` → `mostrarCarrito()` | Funciona | Lo ejecuté (la página muestra el texto real con "Última actualización: noviembre de 2026", y el carrito muestra el resumen con "Leer términos completos". Ojo: la aceptación **no se guarda** en el backend) |
| **[Frontend] Historial de compras y "Cancelar compra"** | `app/compras.js` → `mostrarHistorial()`, `cancelarCompra()` | Funciona | Lo ejecuté (pantalla + backend real: muestra "Esperando al repartidor" y "Delivery · inDrive – Av. Javier Prado 500, San Isidro". La ventana de cancelación es del 10/11) |
| **[Frontend] Pedir la reseña automáticamente** después de un recojo o de un delivery entregado | `app/resenas-portal.js` → `avisarResenasPendientes()`, `abrirResena()` | Funciona | Lo ejecuté (pantalla + backend real: apareció también después de un delivery entregado) |
| **[Frontend] Notificaciones del consumidor** y contador de la campana | `app/notificaciones.js` → `cargar()`, `marcar()`; `app/portal.js` → `actualizarCampana()` | Funciona | Lo ejecuté (pantalla + backend real, con todas las notificaciones leídas: ya no aparece "null") |
| **[Frontend] Perfil**: editar datos, cambiar contraseña y subir foto | `app/perfil.js` → `montarPerfil()` | Funciona | Lo ejecuté (el 10/11: los datos reales en pantalla y guardar por API. `perfil.js` no cambió el día 1) |
| **[Frontend] Cerrar sesión del consumidor** | `app/portal.js` → `iniciarPortal()` (botón `#salir`) | Funciona | Lo ejecuté (el 10/11, pantalla + backend real) |
| **[Frontend] Compra presencial escaneando el QR** (efectivo, tarjeta o Yape) | `app/qr.js` → `iniciar()`, `actualizarPago()` | Funciona | Lo ejecuté (el 10/11 con efectivo. `qr.js` solo cambió para usar `tarjetaVigente()`) |
| **[Frontend] Panel del trabajador**: pack vigente, stock y ganancias | `app/trabajador.js` → `cargar()`, `pintarVigente()` | Funciona | Lo ejecuté (pantalla + backend real) |
| **[Frontend] Crear un pack nuevo en 3 pasos** o publicar uno ya registrado | `app/trabajador.js` → `prepararFormulario()`, `validarPaso()`, `publicar()`, `qrOferta()`, `alternarOrigen()` | Funciona | Lo ejecuté (el 10/11, pantalla + backend real. El 11/11 se vio que el formulario se bloquea con el local suspendido) |
| **[Frontend] Validar un código y confirmar la entrega** (recojo, efectivo o "Entregado al repartidor") | `app/trabajador.js` → `pintarCompra()`, `confirmar()`; `app/trabajador.html` | Funciona | Lo ejecuté (pantalla + backend real: el ejemplo del campo ahora es `ABBAFE9E-4869-4A`. `7cc5a96fc8294d`, sin guion y en minúsculas, encontró el pedido. Muestra "Delivery: Rappi – Av. Prueba 45, Ate" y el botón "Entregado al repartidor". Ya no aparece "null") |
| **[Frontend] Aviso de local suspendido** en el panel del trabajador | `app/trabajador.js` → `pintarSuspension()`, `pintarDisponibilidadPublicacion()`; `app/comun.js` → `conPunto()` | Funciona | Lo ejecuté (pantalla + backend real, con el local suspendido de verdad. Sale la banda "Local suspendido por documentos vencidos" con el motivo completo y la insignia "Oculto mientras el local esté suspendido". Los pasos de "Crear nuevo pack" se bloquean con "No puedes publicar mientras el local esté suspendido". Aun así, se pudo entregar un pedido pagado antes de la suspensión) |
| **[Frontend] Registrar la empresa y su primer local**, con "Usar mi ubicación actual" | `app/empresa.js` → envío de `#form-empresa`, `iniciarEmpresa()`, envío de `#form-local` | Funciona | Lo ejecuté (el 10/11 por API real. El 11/11 se vio el botón "Usar mi ubicación actual" en el formulario, pero no se usó; registrar con coordenadas se probó por API) |
| **[Frontend] Ubicación del local** en su detalle ("Ubicación registrada" / "Sin ubicación" y "Actualizar ubicación") | `app/empresa.js` (detalle del local) | Funciona | Lo ejecuté (pantalla + backend real, con ubicación simulada: "Actualizar ubicación" guardó la nueva latitud y longitud y avisó "Ubicación actualizada") |
| **[Frontend] Adjuntar documentos y enviar la solicitud** | `app/empresa.js` → `abrirLocal()`, `seccionAfiliacion()`, `formularioDocumento()`, `botonEnviar()` | Funciona | Lo ejecuté (el 10/11, pantalla + backend real. El 11/11 se usó lo mismo para la renovación) |
| **[Frontend] Renovar documentos** vencidos o por vencer | `app/empresa.js` → `seccionVigentes()` | Funciona | Lo ejecuté (pantalla + backend real: con el documento por vencer, la sección "Renovación" pidió ese tipo. Se adjuntó, se envió ("Renovación en revisión"), el administrador la aprobó y el documento quedó "vigente". Con el local suspendido, el backend aceptó solo el tipo vencido y rechazó otro con 409) |
| **[Frontend] Resumen de la empresa** con indicadores, impacto, alertas y reseñas | `app/empresa.js` → `cargarResumen()`, `pintarAlertas()`, `tarjetaIndicadores()`; `app/panel.js` → `cifraImpacto()` | Funciona | Lo ejecuté (pantalla + backend real: "Comida recuperada 4,0 kg" y "CO₂ evitado 10,0 kg" en el total y por local, y 0,0 kg en el local sin entregas) |
| **[Frontend] Notificaciones de la empresa** | `app/empresa.js` → `cargarAvisos()`; `app/panel.js` → `listaAvisos()` | Funciona | Lo ejecuté (el 10/11, pantalla + backend real. El 11/11 llegaron por API "Documento por vencer", "Local suspendido" y "Renovación aprobada") |
| **[Frontend] Revisar una solicitud como administrador** (afiliación o renovación) | `app/administrador.js` → `cargarSolicitudes()`, `abrirSolicitud()`, `formularioResolucion()` | Funciona | Lo ejecuté (pantalla + backend real: una afiliación nueva sin "null" y una renovación con su banda "Renovación", aprobada desde la pantalla) |
| **[Frontend] Lista de locales para el administrador** con filtro por estado | `app/administrador.js` → `cargarEstablecimientos()`, `pintarEstablecimientos()` | Funciona | Lo ejecuté (el 10/11, pantalla + backend real) |
| **[Frontend] Redirigir de `negocio.html`** al portal correcto | `app/negocio.html` (script en línea) | Funciona | Lo ejecuté (el 4/11. El archivo no cambió) |

## Otros hallazgos (no son funciones, pero conviene saberlos)

- **Corregido el día 1 y comprobado hoy:**
  - el "null" en tres pantallas;
  - el ejemplo del código sin guion;
  - el doble punto en el aviso de rechazo;
  - el recordatorio que no se apagaba;
  - el mensaje de "El local debe estar aprobado";
  - los términos de relleno;
  - el delivery que se anunciaba sin existir;
  - "Cerca de mí";
  - el impacto vacío;
  - el límite de intentos.
- **Sigue pendiente o es una decisión del equipo:**
  - La contraseña de MySQL que estuvo en `application-mysql.properties` **sigue en el historial de git**. Conviene cambiarla en MySQL.
  - La contraseña del administrador demo sigue escrita en `application.properties`. El equipo decidió dejarla así.
  - Un local se puede aprobar con un solo documento. No se verificó si es intencional.
  - El bloqueo de inicio de sesión es por cuenta y no por IP: quien conozca un correo puede dejar esa cuenta sin acceso 5 minutos. Además, el contador vive en la memoria del servidor y se borra al reiniciarlo.
  - La aceptación de los términos no queda registrada en el backend.
  - El token del QR sigue siendo fijo durante la oferta. Ahora cada consumidor solo puede tener una reserva en efectivo pendiente, pero con varias cuentas todavía se puede apartar stock.
  - El consumidor y la empresa siguen compartiendo la sesión de persona.
- **No probado:**
  - el perfil MySQL completo: la conexión con `application-local.properties` y la búsqueda con tildes;
  - "Cerca de mí" con un GPS real;
  - pagar con tarjeta desde `qr.html`;
  - el comportamiento de la hora en un servidor que no esté en hora de Perú.
- **Las funciones nuevas no tienen pruebas automáticas:** delivery, "Cerca de mí", impacto, límite de intentos y una sola reserva en efectivo se comprobaron a mano y con scripts, pero `mvnw test` no las cubre. El registro del día 1 dice que esas pruebas quedan para el día 3.
- **[3-pendientes.md](3-pendientes.md) quedó desactualizado:** describe los 9 puntos como estaban el 10/11. Esta tabla tiene su estado actual.

## Cómo repetir esta revisión

Arrancar el servidor con los datos de demostración y abrir `http://localhost:8080`:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="dev,demo"
.\mvnw.cmd spring-boot:run
```

Algunas cuentas de la demo:

- consumidor: `demo.consumidor1@sazloop.test`;
- trabajador: `demo.miraflores`;
- dueña: `duena.aurora@sazloop.test`;
- administrador: `demo.admin@sazloop.test`.

Su contraseña está en `backend/src/main/java/com/sazloop/demo/DatosDemoH2.java` (constante `CLAVE`).

Para probar el vencimiento de documentos y de pedidos sin esperar días:

1. Abrir `http://localhost:8080/h2-console`.
2. Conectarse con la URL `jdbc:h2:mem:sazloop` y el usuario `sa`, sin contraseña.
3. Cambiar `fecha_vencimiento` en `documentos_establecimiento`, o `fin_recojo` en `ofertas_pack`.
4. Esperar un minuto a que corra la revisión automática.

Repetir las pruebas del backend (siempre con el perfil `dev`, para no escribir en MySQL):

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd test
```
