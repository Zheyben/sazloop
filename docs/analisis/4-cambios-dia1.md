# Cambios del día 1 (corrección del código)

Este archivo resume todo lo que se cambió en el código el día 1. Sirve el día 2 para corregir el informe. Está escrito en lenguaje simple.

- **Rama de trabajo:** `feature/avance3franquiño` (sale de `main`, que estaba al día).
- **Resultado de las pruebas:** las 14 pruebas existentes pasan con el perfil `dev`. No se escribieron pruebas nuevas (eso es del día 3).
- **Revisión de JavaScript:** `node --check` sin errores en todos los `.js` de `frontend/`.
- **Cifras de la API:** antes eran **53 servicios web en 15 controladores**. Ahora son **54 servicios web en 15 controladores** (se agregó uno: la ubicación del local; no hay controladores nuevos).

---

## 1. Resumen por tarea

### Tarea 1 · La palabra «null» en pantallas
- **Qué se cambió:** se cambió `: null` por `: ''` en los 3 lugares donde se pasaba `null` a `replaceChildren()`:
  - `app/administrador.js` → `abrirSolicitud()` (banda de renovación).
  - `app/trabajador.js` → `pintarCompra()` (confirmación de entrega).
  - `app/notificaciones.js` → `cargar()` (botón «Marcar todas como leídas»).
- **Revisión de todo el frontend:** se revisaron todas las llamadas a `replaceChildren(`, `append(` y `prepend(`. Los demás `? … : null` están dentro de `el()`, que ya descarta `null`, así que no escribían «null». No hizo falta tocar nada más.
- **Cómo se comprobó (con el navegador, perfil `dev,demo`):** administrador abre una solicitud nueva, trabajador consulta un código pagado sin entregar, consumidor pulsa «Marcar todas como leídas» y recarga. En los tres casos no aparece «null».

### Tarea 2 · Detalles menores
| Punto | Qué se hizo | Archivos |
|---|---|---|
| a. Código con guion | El servidor quita espacios y guiones y pasa a mayúsculas antes de buscar, y compara contra el código guardado normalizado igual (`upper(replace(codigo,'-',''))`). El ejemplo del campo ahora es `ABBAFE9E-4869-4A`. | `CodigoRecojoRepository`, `CompraRecojoService`, `CompraPresencialService`, `trabajador.html` |
| b. Doble punto | Se quitan los puntos y espacios del final del motivo antes de unirlo al texto (también en el aviso de renovación rechazada). | `RevisionAfiliacionService` |
| c. Recordatorio leído | Al **entregar**, **cancelar** o **vencer (no recogido)** el pedido, su recordatorio «Recoge tu pedido» se marca como leído. | `AvisosReservaService`, `NotificacionService`, `NotificacionRepository`, `CompraRecojoService` |
| d. Tarjeta | Las dos pantallas (`qr.js` y `compras.js`) **ya usaban** la misma función `tarjetaVigente()` de `comun.js`, que acepta el mes actual y rechaza el mes anterior. No hizo falta cambiar código; solo se comprobó. | (sin cambios) |
| e. Local suspendido | Se mantiene permitido crear la cuenta del trabajador; el mensaje ahora dice «El local debe haber sido aprobado». | `CuentaLocalService` |
| f. La hora | Se fija `America/Lima` para todo el sistema: `TimeZone.setDefault` en un bloque estático de `SazloopApplication` (así también vale en las pruebas) y, en `application.properties`, `spring.jackson.time-zone` y `spring.jpa.properties.hibernate.jdbc.time_zone`. Las partes que ya usaban `ZoneId.of("America/Lima")` (`PanelLocalService`, `VigenciaDocumentosService`) siguen igual. | `SazloopApplication`, `application.properties` |
| g. Una reserva en efectivo | Si el consumidor ya tiene una reserva en efectivo pendiente (retención activa y no vencida), la segunda da **409** con el mensaje «Ya tienes una reserva en efectivo pendiente. Confírmala en el local o espera a que venza». | `CompraPresencialService`, `RetencionStockRepository` |
| h. Aviso «no recogido» | Se probó en la tarea 9.3. | — |

**Comprobado:** código sin guion y en minúsculas se encuentra; rechazo con «Documento ilegible.» da «…ilegible. Puedes adjuntar…» (un solo punto); recordatorio de pedido entregado queda leído; segunda reserva en efectivo da 409.

### Tarea 3 · Credenciales fuera del repositorio
- `application-mysql.properties` ahora trae valores de ejemplo (`root` / `cambia_esto`) y la línea `spring.config.import=optional:file:./application-local.properties`.
- Nuevo `backend/application-local.properties.example` (se sube) y `backend/application-local.properties` en el `.gitignore` (no se sube).
- README: nueva sección **«Configurar MySQL»** con los pasos.
- `AdministradorService`: el comentario ya dice que son credenciales de demostración y que la contraseña de MySQL va en `application-local.properties`. La cuenta del administrador demo (`application.properties`) se queda igual.
- **Comprobado:** al arrancar con el perfil `mysql` desde `backend/`, Spring carga `application-local.properties` (se vio con el registro de configuración y el usuario de prueba llegó a MySQL); `git status` no lo muestra; el perfil `dev` arranca igual que antes. Buscar `password=` en el repositorio solo muestra valores de ejemplo y la cuenta demo del administrador.
- ⚠️ **Importante:** la contraseña real que estaba antes en `application-mysql.properties` **sigue en el historial de git** (los commits viejos). Quitarla del archivo no la borra del historial. Se recomienda **cambiar la contraseña de MySQL** de esa cuenta.

### Tarea 4 · Términos y condiciones
- `app/terminos.html`: términos reales y cortos, con «Última actualización: noviembre de 2026» y los 8 puntos pedidos, en una lista numerada.
- `app/compras.js` (carrito): se reemplazó el texto copiado por un resumen de 2 líneas y el enlace «Leer términos completos» (abre `terminos.html` en pestaña nueva). La casilla sigue siendo obligatoria.
- `app/qr.html` ya enlazaba a `terminos.html`: funciona.
- No queda «Lorem ipsum» ni «Texto provisional» en `frontend/`. No se guarda la aceptación en el backend.

### Tarea 5 · Límite de intentos de inicio de sesión
- Nuevo componente `shared/LimiteIntentosLogin`, usado por los 3 inicios de sesión (persona, trabajador y administrador). Contador en memoria (`ConcurrentHashMap`), sin tabla; clave = tipo + identificador en minúsculas y sin espacios (no por IP).
- **5 fallos seguidos → bloqueo de 5 minutos → 429** con «Demasiados intentos fallidos. Espera 5 minutos e inténtalo de nuevo.», aunque la contraseña sea correcta. Pasado el bloqueo el contador vuelve a cero; un acceso correcto lo pone en cero.
- Valores en `application.properties`: `sazloop.login.max-intentos=5` y `sazloop.login.bloqueo-minutos=5`.
- **Mismo tiempo de respuesta:** `CodificadorContrasena` crea un hash de relleno al arrancar y, si el correo/usuario no existe, igual calcula PBKDF2 (`rellenar()`). Aplicado en los 3 servicios (`SesionUsuarioService`, `CuentaLocalService`, `AdministradorService`).
- `app/accesos.js`: cuando la respuesta es 429 se muestra el mensaje del servidor.
- **Comprobado** (con `bloqueo-minutos=1`): intentos 1 a 5 dan 401, el 6.º da 429; la contraseña correcta durante el bloqueo también da 429; al terminar entra. Igual en los 3 accesos. Un correo inexistente también se bloquea.

### Tarea 6 · Impacto ambiental
- **Fórmula:** kilos = peso por tamaño × unidades (pequeño 0,5 kg, mediano 1,0 kg, grande 1,5 kg); **CO₂ evitado (kg) = kilos × 2,5**.
- **Fuente del 2,5:** FAO (2013), *Food Wastage Footprint: Impacts on Natural Resources*: unos 3.300 millones de toneladas de CO₂ equivalente ÷ unos 1.300 millones de toneladas de comida desperdiciada al año ≈ 2,5 kg de CO₂e por kilo.
- Los pesos son **una suposición del proyecto**, fijos en el código (constantes), sin campos nuevos en los packs. Un tamaño desconocido usa 1,0 kg.
- Nuevo `experiencia/service/ImpactoService.registrar(reserva)`, usado en `recoger()` (recojo y delivery) y en `confirmarEfectivo()`. Guarda `alimentoRecuperadoKg`, `co2EvitadoKg`, `estimacionDisponible = true` y `metodologia` (≈ 135 caracteres; la columna admite 300).
- `IndicadoresEmpresaService` suma kilos y CO₂ **por local y en total** (los registros antiguos con valores vacíos cuentan 0). Solo se muestra en el portal de empresa (`app/empresa.js`), junto a «Packs entregados»: «Comida recuperada: X kg» y «CO₂ evitado: Y kg» con 1 decimal (tarjetas verdes nuevas, `cifraImpacto()` en `panel.js` y estilos en `panel.css`).
- **Comprobado:** 2 unidades de pack mediano → 2,0 kg y 5,0 kg de CO₂; el resumen de la empresa muestra por local y total.

### Tarea 7 · Búsqueda en la base de datos + «Cerca de mí»
**7.1 Búsqueda**
- Nuevo `ofertas/repository/OfertaBusquedaRepository` (JPQL dinámica): la base de datos filtra por oferta activa y vigente, local activo, stock > 0 y precio máximo (con `exists`), distrito y dirección (`like`, sin importar mayúsculas) y calificación mínima (suma ≥ mínimo × cantidad de reseñas, en una subconsulta). Trae oferta, pack, local y dirección con `fetch join`; las colecciones (variantes, alérgenos, restricciones) se cargan por lotes (`default_batch_fetch_size=50`) y las calificaciones de todos los locales salen de **una sola** consulta agrupada.
- **Resultados idénticos:** se grabaron 15 búsquedas con el código anterior (sin filtros, cada filtro por separado y combinaciones; por ejemplo «Ate» con calificación 3,5 → 1 pack) y se repitieron con el código nuevo: **las 15 respuestas son exactamente iguales** (mismo contenido y mismo orden).
- **Consultas:** antes ≈ 1 + 7 por oferta; ahora una búsqueda hace **8 consultas fijas**, sin importar cuántas ofertas haya.

**7.2 «Cerca de mí»**
- `direcciones` tiene dos columnas nuevas **opcionales**: `latitud` y `longitud`. Validación: latitud −90 a 90, longitud −180 a 180.
- Registro de local (`#form-local`): botón opcional «Usar mi ubicación actual». Detalle del local: «Ubicación registrada» / «Sin ubicación» y botón «Actualizar ubicación» (servicio nuevo, solo el dueño).
- Búsqueda: `GET /api/ofertas?lat=&lng=`. El backend calcula la distancia en línea recta (**Haversine**) con las ofertas ya filtradas, ordena de más cerca a más lejos, deja los locales **sin coordenadas al final** y devuelve `distanciaKm` (vacío si no hay ubicación o el local no tiene coordenadas). Sin radio máximo. La ubicación del consumidor no se guarda ni se pone en la URL.
- Pantalla: casilla activada, pide permiso; si se niega sale «Activa la ubicación para usar este filtro» y la casilla se desmarca. Cada tarjeta muestra «a 1,2 km» o «Distancia no disponible» (solo con el filtro activo).
- Datos demo: coordenadas aproximadas de cada local (Miraflores, San Borja y dos en Ate).
- **Comprobado (navegador con ubicación simulada):** orden por distancia correcto, mensaje al negar el permiso, dueño registra un local con ubicación y actualiza la de uno existente; otro dueño recibe 404; valores fuera de rango, 400.

### Tarea 8 · Delivery simulado
- **Backend:** cada reserva tiene `modalidad` (`RECOJO` por defecto / `DELIVERY`), `empresaDelivery` (`PEDIDOSYA`, `RAPPI`, `INDRIVE`), `entregaDistrito`, `entregaDireccion` y `entregaReferencia`. `CompraRequest` y `CompraMultipleRequest` los reciben (valen para toda la compra: todos los pedidos del carrito comparten modalidad y dirección). Sin modalidad = recojo (se mantuvieron los constructores antiguos para no romper nada).
- Validación: `DELIVERY` sin empresa, distrito o dirección → **400** con mensaje claro. La compra presencial con QR **no** admite delivery (400).
- Sazloop no cobra el envío: el total, la boleta y la comisión no cambian. «Entregado al repartidor» usa el mismo código y el mismo `recoger()` (cuenta como recogido: impacto, reseña y paneles). Mismas reglas de horario, cancelación (50 %) y «no recogido».
- **Avisos:** el recordatorio dice «Tu pedido llegará con PedidosYa a [dirección], [distrito]. Código: X…»; el aviso de no recogido de un delivery dice «Pedido no entregado al repartidor: …».
- **Frontend:** carrito con «Recojo en el local» (por defecto) o «Delivery» (selector de empresa con tres botones de color, distrito, dirección, referencia opcional, nota del costo del envío; el botón de pagar no se activa hasta llenar lo obligatorio); la boleta y «Mis compras» muestran la modalidad (y la etiqueta «Esperando al repartidor» / «Entregado al repartidor»); el trabajador ve «Delivery: PedidosYa – dirección, distrito (referencia)» y el botón «Entregado al repartidor»; `qr.html` y `qr.js` sin cambios; `index.html` sin «PROXIMAMENTE», con «Servicios de delivery disponibles (simulado)» y textos revisados.
- **Comprobado:** delivery sin dirección o sin empresa → 400; compra en lote con delivery guarda todo en cada pedido; sin modalidad → recojo; el trabajador lo ve, lo marca entregado, queda recogido con reseña pendiente; cancelar un delivery devuelve 9,00 de 18,00 (50 %); QR con delivery → 400. También se probó todo en pantalla.

### Tarea 9 · Pantallas sin verificar (probadas en el navegador, perfil `dev,demo`, consola H2 para adelantar el reloj)
- **9.1 Suspensión:** se puso a ayer la fecha del documento de «Sazón Urbano Ate». La revisión automática lo suspendió sola. El trabajador ve la banda roja «Local suspendido por documentos vencidos» con el motivo completo, «Oculto mientras el local esté suspendido», el formulario de «Crear nuevo pack» bloqueado con «No puedes publicar mientras el local esté suspendido», y **sí puede entregar** un pedido pagado antes de la suspensión. El consumidor no ve el local en la búsqueda ni en Productos. **Corrección encontrada:** faltaba un punto entre el motivo y «Puedes validar…» (y entre el motivo y «Suspendido desde…» en el portal de empresa). Se agregó `conPunto()` en `comun.js`.
- **9.2 Renovación:** documento de «Aurora Miraflores» a vencer en 3 días → apareció «Por vencer» y llegó el aviso a Notificaciones. La dueña adjuntó el documento renovado y lo envió; el administrador lo aprobó (con su banda de renovación, sin «null»); la tabla pasó a «Vigente». Con el local suspendido, la pantalla pidió **solo** los documentos vencidos y, al aprobar, el local volvió a «Activo» y a la búsqueda.
- **9.3 No recogido:** una oferta con fin de horario en 2 minutos, un pedido de recojo y otro de delivery sin recoger. Al pasar el horario y el minuto de revisión, ambos pasaron a «expirado» y cada consumidor recibió su aviso («Pedido no recogido…» y «Pedido no entregado al repartidor…»), y el recordatorio quedó leído.
- Atajos usados (no cambian el sistema): en vez de adjuntar y aprobar el primer documento con fecha, se puso la fecha por la consola H2; y la oferta de 2 minutos se logró acortando su horario en H2 (el archivo `3-pendientes.md` que citaba el plan no está en el repositorio).

---

## 2. Base de datos

No hay scripts SQL ni migraciones en el repositorio. El esquema lo genera Hibernate: `create-drop` en `dev` (H2) y `update` en `mysql`, así que **los campos nuevos aparecen solos**. No hay **tablas nuevas**.

**Campos nuevos (para el diagrama entidad-relación):**

| Tabla | Campo nuevo | Tipo | Nulo | Para qué |
|---|---|---|---|---|
| `direcciones` | `latitud` | decimal(10,6) | sí | «Cerca de mí» |
| `direcciones` | `longitud` | decimal(10,6) | sí | «Cerca de mí» |
| `reservas` | `modalidad` | texto(10): `RECOJO` / `DELIVERY` | sí (nulo = recojo) | Delivery simulado |
| `reservas` | `empresa_delivery` | texto(15): `PEDIDOSYA` / `RAPPI` / `INDRIVE` | sí | Delivery simulado |
| `reservas` | `entrega_distrito` | texto(80) | sí | Delivery simulado |
| `reservas` | `entrega_direccion` | texto(180) | sí | Delivery simulado |
| `reservas` | `entrega_referencia` | texto(250) | sí | Delivery simulado |

**Tablas que ya existían y ahora se llenan:** `impactos_co2_evitado` (`alimento_recuperado_kg`, `co2_evitado_kg`, `estimacion_disponible = true`, `metodologia`). No cambia su estructura.

---

## 3. Clases nuevas o modificadas (para el diagrama de clases)

**Nuevas**
- `shared.LimiteIntentosLogin` (límite de intentos de inicio de sesión).
- `experiencia.service.ImpactoService` (cálculo y registro del impacto).
- `ofertas.repository.OfertaBusquedaRepository` (búsqueda con filtros en la base de datos; clase interna `Calificacion`).
- `identidad.dto.UbicacionRequest` (latitud y longitud).

**Modificadas**
- Dominio: `Direccion` (+ `latitud`, `longitud`); `Reserva` (+ `modalidad`, `empresaDelivery`, `entregaDistrito`, `entregaDireccion`, `entregaReferencia`; enumeraciones internas `Modalidad` y `EmpresaDelivery`; métodos `getModalidad()` y `esDelivery()`).
- DTO: `DireccionRequest` (+ coordenadas), `EstablecimientoResponse` (+ coordenadas), `OfertaResponse` (+ `distanciaKm`), `CompraRequest` y `CompraMultipleRequest` (+ campos de delivery), `CompraResponse` (+ campos de delivery), `IndicadoresEmpresaService.Indicadores` (+ `alimentoRecuperadoKg`, `co2EvitadoKg`).
- Servicios: `SesionUsuarioService`, `CuentaLocalService`, `AdministradorService` (límite de intentos y tiempo igual); `CodificadorContrasena` (+ `rellenar()`); `CompraRecojoService` (delivery, impacto, código normalizado, cierre del recordatorio); `CompraPresencialService` (sin delivery, una reserva en efectivo, impacto); `AvisosReservaService` (avisos de delivery y `cerrarRecordatorio()`); `NotificacionService` (+ `leerPorReferencia()`); `RevisionAfiliacionService` (punto final del motivo); `EmpresaService` (+ `actualizarUbicacion()`, coordenadas al registrar); `IndicadoresEmpresaService` (kilos y CO₂); `OfertaService` (`buscar` con filtros en la base de datos y distancia).
- Repositorios: `CodigoRecojoRepository` (búsqueda normalizada), `RetencionStockRepository`, `NotificacionRepository`, `ImpactoCO2EvitadoRepository` (+ `findByLocal`).
- Otros: `SazloopApplication` (zona horaria), `EmpresaController` (+ servicio de ubicación), `OfertaController` (+ `lat`, `lng`), `demo.DatosDemoH2` (coordenadas).

---

## 4. Servicios web nuevos o modificados

**Nuevo (1)**
| Método y ruta | Para qué sirve |
|---|---|
| `PUT /api/empresas/actual/establecimientos/{id}/ubicacion` | Guarda la ubicación (latitud y longitud) de un local. Solo el dueño. 404 si el local no es suyo; 400 si las coordenadas están fuera de rango. |

**Modificados**
| Método y ruta | Cambio |
|---|---|
| `GET /api/ofertas` | Filtros hechos en la base de datos; acepta `lat` y `lng` («Cerca de mí»); la respuesta trae `distanciaKm`. 400 si falta una de las dos o están fuera de rango. |
| `POST /api/compras` y `POST /api/compras/lote` | Aceptan `modalidad`, `empresaDelivery`, `entregaDistrito`, `entregaDireccion`, `entregaReferencia`; 400 si el delivery está incompleto. |
| `GET /api/compras/mias`, `GET/POST /api/local/recojos/{codigo}…`, `POST /api/local/efectivo/{codigo}/confirmar` | La respuesta trae la modalidad y los datos de entrega; el código se acepta con o sin guion y en mayúsculas o minúsculas. |
| `POST /api/compras/presenciales` y `/api/compras/presenciales/efectivo` | 400 si piden delivery; el efectivo da **409** si ya hay una reserva en efectivo pendiente. |
| `POST /api/sesion`, `POST /api/local/sesion`, `POST /api/administradores/sesion` | Responden **429** tras 5 intentos fallidos (bloqueo de 5 minutos). |
| `POST /api/empresas/actual/establecimientos`, `GET /api/empresas/actual/establecimientos` | La dirección acepta y devuelve `latitud` y `longitud`. |
| `GET /api/empresas/actual/indicadores` | Trae `alimentoRecuperadoKg` y `co2EvitadoKg` por local y en total. |
| `POST /api/establecimientos/{id}/cuenta` | Solo cambió el texto del error («El local debe haber sido aprobado»). |

**Cifra para el informe:** 54 servicios web en 15 controladores.

---

## 5. Funciones nuevas que el informe debe describir

1. **Delivery simulado:** PedidosYa, Rappi o inDrive, solo en compras desde la web; Sazloop no cobra el envío; «Entregado al repartidor» cuenta como recogido; mismas reglas de horario y cancelación.
2. **«Cerca de mí»:** ubicación opcional del local (GPS del navegador), distancia de Haversine, orden por cercanía, locales sin ubicación al final, la ubicación del consumidor no se guarda. Búsqueda filtrada en la base de datos (8 consultas fijas).
3. **Cálculo del impacto:** kilos = peso estimado por tamaño × unidades (0,5 / 1,0 / 1,5 kg); CO₂ = kilos × 2,5 (FAO, 2013, *Food Wastage Footprint*); se muestra solo en el portal de empresa. Los pesos son una suposición del proyecto.
4. **Límite de intentos de inicio de sesión:** 5 fallos → 5 minutos de bloqueo (429), contador en memoria, sin tabla, sin contar por IP, mismo tiempo de respuesta si la cuenta no existe.
5. **Términos y condiciones** reales (una página, 8 puntos).
6. **Una sola reserva en efectivo pendiente** por consumidor.
7. **Zona horaria de Perú (America/Lima)** para todo el sistema.
8. Además: el código de recojo se acepta con o sin guion; el recordatorio de recojo se marca leído al cerrar el pedido; credenciales de MySQL en un archivo local.

---

## 6. Decisiones tomadas durante la implementación (no cubiertas por el plan)

- **Ruta de la ubicación:** el plan decía «por ejemplo `PUT /api/empresa/establecimientos/{id}/ubicacion»`. Se usó `PUT /api/empresas/actual/establecimientos/{id}/ubicacion` para ser coherente con las rutas que ya existen de la empresa.
- **Columnas de delivery con nulos permitidos:** así no se rompen las reservas anteriores en MySQL (`ddl-auto=update` no puede agregar una columna obligatoria sin valor). `getModalidad()` devuelve `RECOJO` cuando está vacía.
- **«Reserva pendiente»** (tarea 2g) = retención de stock activa y no vencida del consumidor, en cualquier local; así una reserva ya vencida pero aún no barrida por la revisión automática no bloquea.
- **Se mantuvieron constructores antiguos** (`DireccionRequest` de 7 datos, `CompraRequest` de 3, `CompraMultipleRequest` de 2) y una sobrecarga `OfertaService.buscar(4 datos)` para que las 14 pruebas existentes sigan sin cambios.
- **Búsqueda y MySQL:** en MySQL la comparación de texto del `like` no distingue mayúsculas **ni tildes** (según la configuración de la base), así que «sazon» también encontraría «Sazón». En H2 (perfil `dev`) las tildes cuentan. Es un efecto menor y más cómodo para el usuario. La calificación mínima se filtra con un margen mínimo en la base de datos y se confirma con el valor exacto en Java.
- **Aviso de bloqueo:** el mensaje dice «Espera 5 minutos…» y, si se configura 1 minuto, «Espera 1 minuto…». El contador no caduca solo por tiempo: se reinicia con un acceso correcto o al pasar un bloqueo.
- **Impacto con tamaño desconocido:** 1,0 kg (no ocurre hoy: solo existen los tres tamaños).
- **Decimales con coma:** las cifras de kg y las distancias se muestran con coma («1,2 km», «2,5 kg») aunque el navegador use punto en es-PE, como pide el plan.
- **Términos:** se redactaron los 8 puntos pedidos; el de delivery ya describe lo que hace el carrito.
- **Se probó con un JDK 21 portátil** (el equipo no tenía Java instalado) y con Edge sin interfaz (para ver las pantallas, con ubicación simulada). Nada de eso queda en el repositorio.
- **Git:** el token que se entregó pertenece a la cuenta `CapiloDev`, que **no tiene permiso de escritura** en `Zheyben/sazloop` (solo lectura). Por eso el `push` no se pudo hacer (ver más abajo). El token no se escribió en ningún archivo del repositorio ni en la configuración de git.

---

## 7. Pruebas a mano pendientes

- **Conexión real a MySQL con `application-local.properties`:** se comprobó que Spring carga el archivo y que el usuario llega a MySQL, pero no se probó con la contraseña real de cada integrante. Pasos: copiar el `.example`, escribir la propia contraseña, arrancar con `mvnw.cmd spring-boot:run` desde `backend/`.
- **Búsqueda en MySQL:** la comparación de resultados se hizo en H2 (perfil `dev`). Conviene repetir unas cuantas búsquedas en MySQL.
- **Pantalla de tarjeta de `qr.html`** (tarea 2d): se verificó que usa `tarjetaVigente()`, pero no se pagó con tarjeta de este mes en esa pantalla.
- **Navegadores reales de la demo:** la ubicación se probó con Edge y permisos simulados. Probar una vez con el GPS real y con el permiso negado en el navegador de la demo (`http://localhost:8080`).
- **Pruebas automáticas:** correr siempre con `SPRING_PROFILES_ACTIVE=dev` (si no, 6 de 7 archivos escriben en la base MySQL real).
