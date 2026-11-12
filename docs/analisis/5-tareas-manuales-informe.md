# Tareas manuales del informe (después de los cambios del día 1)

El texto de `Proyecto_final.docx` ya se corrigió para que coincida con el código del día 1 (ver `4-cambios-dia1.md`). Lo que queda son tres tareas que no son de texto: **diagramas**, **capturas de pantalla** y **medición de rendimiento**. Este archivo explica qué hacer en cada una, paso a paso.

Al terminar las tres, actualiza la tabla de contenido del informe (al final de este archivo).

## Estado (11 de noviembre de 2026)

| Tarea | Estado | Qué se hizo |
|---|---|---|
| E1.1 Casos de uso | Hecho | 6 textos cambiados, 3 extensiones `«extend»` nuevas (la condición va dentro de cada óvalo) y nota «(recojo o entrega completada)» bajo CU/RF-23. Se borró un texto oculto «include» del rectángulo de Transacciones que quedaba a la vista al ensanchar el módulo. |
| E1.2 Clases | Hecho | `Direccion`, `Reserva` (5 atributos y 2 métodos nuevos) e `ImpactoCO2Evitado` (opcional d). `Recojo` y sus etiquetas bajaron 112 px para que no se monte con `Reserva`. No se dibujaron cajas `«enumeration»`. |
| E1.3 Entidad-relación | Hecho | 7 columnas en el `.txt`. La imagen se generó desde ese archivo con la misma distribución por módulos de la figura anterior (`codigos_recojo` y `recojos` bajaron para dejar sitio a `reservas`). La fuente de la Figura 03 dice ahora «a partir del modelo en formato DBML». |
| Imágenes de los diagramas | Hecho | PNG en `docs/diagramas/` y en las Figuras 01 a 03 del informe (se quitaron las copias SVG viejas). |
| E2.1, E2.2, E2.4, E2.6, E2.7 | Hecho | Figuras 09, 10, 12, 17 (con la nueva parte c) y 18, completas con los datos demo, y sus pies de figura. |
| E2.3 (Figura 11) y E2.5 (Figura 13) | No hecho | Eran opcionales; siguen las capturas del 12 de octubre. La introducción de 3.8 dice qué figuras son de cada fecha. |
| E3.2 Medición A | Hecho | Ver resultados abajo; ya están en la fila «Rendimiento» de 3.8.6. |
| E3.3 Medición B | En parte | Se midió con 1, 10 y 20 usuarios simultáneos, pero en H2. **Falta repetirla con MySQL** (no hay acceso a la base de cada integrante). 3.5.1 lo dice. |
| Tabla de contenido | Pendiente | El `.docx` pide actualizar los campos al abrirlo: acepta el aviso de Word, o usa clic derecho → Actualizar campos → Actualizar toda la tabla. |

**Resultados de la medición del 11/11/2026** (Intel Core i5-12450HX, 24 GB de RAM, perfil `dev,demo`, H2):

| Prueba | Repeticiones | Mediana | p95 | Máximo |
|---|---|---|---|---|
| Búsqueda con filtros (`distrito=Ate&precioMaximo=20`) | 100 | 5,2 ms | 7,3 ms | 24,2 ms |
| Búsqueda «Cerca de mí» (`lat`/`lng`) | 100 | 6,8 ms | 10,2 ms | 21,6 ms |
| Búsqueda sin filtros | 100 | 5,4 ms | 6,6 ms | 7,9 ms |
| Compra simulada (`POST /api/compras/lote`, Yape) | 59 | 8,3 ms | 12,9 ms | 27,2 ms |
| 1 usuario, 15 s (búsquedas alternadas) | 3 898 | 3,7 ms | 4,6 ms | 70,1 ms |
| 10 usuarios simultáneos, 20 s | 57 514 | 3,3 ms | 4,8 ms | 66,8 ms |
| 20 usuarios simultáneos, 30 s | 103 885 | 5,5 ms | 8,4 ms | 75,2 ms |

Sin errores en ninguna prueba. Para que la compra no agotara el stock, cada compra medida se canceló después (la cancelación no se midió).

---

## E1. Diagramas (casos de uso, clases y entidad-relación)

Los archivos editables están en `docs/diagramas-editables/`:

| Figura del informe | Archivo editable | Herramienta |
|---|---|---|
| Figura 01. Diagrama de casos de uso (sección 3.3) | `01-Diagrama de casos de uso.drawio` | draw.io (app.diagrams.net o la app de escritorio) |
| Figura 02. Diagrama de clases (sección 3.4) | `02-Diagrama de clases.drawio` | draw.io |
| Figura 03. Diagrama entidad-relación (sección 3.5.2) | `03-Diagrama ER de base de datos.txt` | dbdiagram.io (código DBML) |

**Regla general:** no se crean RF nuevos. El delivery, «Cerca de mí» y lo demás se agregaron dentro de RF que ya existían (RF-05, RF-14, RF-16, RF-19, RF-21 y RF-26). Por eso se mantienen los rótulos `CU/RF-01` a `CU/RF-26` y la cifra «26 requerimientos».

### E1.1 Diagrama de casos de uso (Figura 01)

**a) Cambiar el texto de 6 casos de uso** (el rótulo `CU/RF-xx` no cambia):

| Rótulo | Texto actual | Texto nuevo | Módulo |
|---|---|---|---|
| CU/RF-05 | Registrar establecimientos | Registrar establecimientos y su ubicación | Identidad |
| CU/RF-14 | Buscar y filtrar establecimientos | Buscar, filtrar y ordenar por cercanía | Ofertas |
| CU/RF-16 | Comprar o reservar un pack | Comprar un pack (recojo o delivery) | Transacciones |
| CU/RF-19 | Retener stock temporalmente | Retener stock temporalmente (una reserva en efectivo por consumidor) | Reservas |
| CU/RF-21 | Entregar pack mediante código | Entregar pack mediante código (al consumidor o al repartidor) | Reservas |
| CU/RF-26 | Registrar indicadores conceptuales | Calcular impacto (kg de comida y CO₂) | Experiencia |

**b) Agregar 3 casos de uso de extensión.** Son óvalos sin rótulo `CU/RF` propio que dependen del caso base. Se dibujan con una flecha punteada `«extend»` que va **desde el nuevo hacia el caso base**, igual que las demás extensiones del diagrama.

| Caso nuevo | Extiende a | Condición (texto junto a la flecha) | Actor que lo usa |
|---|---|---|---|
| Elegir delivery simulado (PedidosYa, Rappi o inDrive) | CU/RF-16 | (modalidad delivery; solo compras desde la web) | CONSUMIDOR |
| Ordenar por cercanía («Cerca de mí») | CU/RF-14 | (permiso de ubicación concedido) | CONSUMIDOR |
| Actualizar ubicación del local | CU/RF-05 | (solo el propietario del local) | PROPIETARIO |

Cada caso nuevo va **dentro del rectángulo de su módulo**: el delivery en Transacciones, «Cerca de mí» en Ofertas y la ubicación en Identidad.

**c) Revisar las relaciones que ya existen:**
- La nota «(recojo completado)» de la relación hacia CU/RF-26 cambia a **«(entrega completada)»**, porque la entrega al repartidor también genera el impacto.
- La nota de CU/RF-22 («Solo para reserva remota confirmada…; reembolso simulado del 50 %») sigue igual: la cancelación de un delivery funciona igual.
- La relación de CU/RF-18 («compra iniciada por QR y pago en efectivo») se mantiene. Si quieres, añade «sin delivery».
- CU/RF-21 ya está unido a PERSONAL DEL ESTABLECIMIENTO. No hace falta un actor «Repartidor»: el repartidor no usa el sistema.

**d) Exportar y reemplazar:**
1. En draw.io: **Archivo → Exportar como → PNG**, con zoom 200 % y borde de 10 px. Desmarca «Fondo transparente» para que el fondo quede blanco al imprimir.
2. Guarda el `.drawio` (sobrescribe el editable) y el PNG. Una carpeta como `docs/diagramas/` sirve para atender la corrección «poner en el repositorio las imágenes en una carpeta en tamaño apropiado».
3. En Word, en la Figura 01: **clic derecho sobre la imagen → Cambiar imagen → Desde un archivo**. Así se conservan el tamaño y la posición.

### E1.2 Diagrama de clases (Figura 02)

Cambian 2 clases de dominio. Opcionalmente se corrige una tercera, que ya estaba incompleta antes del día 1. `ImpactoService`, `LimiteIntentosLogin`, `OfertaBusquedaRepository` y `UbicacionRequest` **no se dibujan**, porque el diagrama solo muestra clases de dominio (entidades).

**a) Clase `Direccion`** (módulo Identidad; en el archivo, la celda con id `1w3Jy9sJn2kyX_fnFrQN-5`). Agrega al final de la lista de atributos:

```
- latitud: BigDecimal
- longitud: BigDecimal
```

Los métodos no cambian. La relación con `Establecimiento` tampoco.

> Nota: en el atributo `id` el texto actual empieza con `+- id: Long`. Sobra el `+`; aprovecha para dejarlo como `- id: Long`.

**b) Clase `Reserva`** (módulo Reservas; celda `rR9DLonVC98vxloOwOsh-67`). Así debe quedar la lista de atributos (los 5 últimos son nuevos):

```
- id: Long
- fechaCreacion: LocalDateTime
- montoTotal: BigDecimal
- origenCompra: OrigenCompra
- estado: EstadoReserva
- resenaOmitida: boolean
- modalidad: Modalidad
- empresaDelivery: EmpresaDelivery
- entregaDistrito: String
- entregaDireccion: String
- entregaReferencia: String
```

> Ojo: hoy `estado: EstadoReserva` y `resenaOmitida: boolean` están pegados en la misma línea. Sepáralos.

Y así la lista de métodos (los 2 últimos son nuevos):

```
+ calcularTotal(): BigDecimal
+ confirmar(): void
+ cancelar(): void
+ marcarListaParaRecojo(): void
+ getModalidad(): Modalidad
+ esDelivery(): boolean
```

`getModalidad()` devuelve `RECOJO` cuando la columna está vacía (reservas anteriores al delivery).

**c) Enumeraciones nuevas.** El diagrama actual no dibuja las enumeraciones (`EstadoReserva` y `OrigenCompra` solo aparecen como tipo de atributo). Para mantener ese estilo basta con los tipos de arriba. Si el profesor pide más detalle, agrega junto a `Reserva` dos cajas con el estereotipo `«enumeration»`:

```
«enumeration» Modalidad
RECOJO
DELIVERY

«enumeration» EmpresaDelivery
PEDIDOSYA
RAPPI
INDRIVE
```

Únelas a `Reserva` con una flecha de dependencia punteada.

**d) Opcional, `ImpactoCO2Evitado`** (módulo Experiencia; celda `rR9DLonVC98vxloOwOsh-114`). En el código (`experiencia/domain/ImpactoCO2Evitado.java`) la clase también tiene `estimacionDisponible` y `metodologia`, que desde el día 1 sí se llenan. Para que coincida, la lista de atributos quedaría:

```
- id: Long
- alimentoRecuperadoKg: BigDecimal
- co2EvitadoKg: BigDecimal
- estimacionDisponible: boolean
- metodologia: String
- fechaRegistro: LocalDateTime
```

**e) Revisar el texto del informe.** La sección 3.4 y la observación n.º 10 de los anexos dicen «31 clases de dominio». Esa cifra **no cambia**: no hay entidades nuevas, y las enumeraciones (si las dibujas) no cuentan como clases persistentes.

**f) Exportar y reemplazar** como en E1.1 d), esta vez en la Figura 02.

### E1.3 Diagrama entidad-relación (Figura 03)

El esquema lo genera Hibernate. No hay tablas nuevas: siguen siendo **31 entidades y 33 tablas**, así que no cambia ninguna cifra del informe. Solo hay 7 columnas nuevas.

**a) En `03-Diagrama ER de base de datos.txt`, tabla `direcciones`** (línea 45 aprox.). Agrega antes de la llave de cierre:

```
  latitud decimal(10,6) [note: 'Opcional, de -90 a 90. Usado por «Cerca de mí»']
  longitud decimal(10,6) [note: 'Opcional, de -180 a 180. Usado por «Cerca de mí»']
```

**b) Tabla `reservas`** (línea 221 aprox.). Agrega después de `fecha_confirmacion datetime`:

```
  modalidad varchar(10) [note: 'RECOJO o DELIVERY; nulo = RECOJO (reservas anteriores)']
  empresa_delivery varchar(15) [note: 'PEDIDOSYA, RAPPI o INDRIVE; solo si modalidad = DELIVERY']
  entrega_distrito varchar(80)
  entrega_direccion varchar(180)
  entrega_referencia varchar(250)
```

Todas aceptan nulos (sin `not null`) para no romper las reservas que ya existían en MySQL. Los tamaños son los mismos del código (`Direccion.java` y `Reserva.java`).

**c) Tabla `impactos_co2_evitado`.** Su estructura no cambia (ya tiene `alimento_recuperado_kg`, `co2_evitado_kg`, `estimacion_disponible` y `metodologia`). Solo cambia que ahora se llena en cada entrega.

**d) Exportar y reemplazar:**
1. Abre dbdiagram.io, crea un diagrama nuevo y pega **todo** el contenido del `.txt`.
2. Revisa que las tablas sigan agrupadas por módulo, como en la figura actual. Si se movieron, arrástralas a su posición de antes.
3. **Export → PNG** (o PDF si la PNG sale borrosa). Reemplaza la imagen de la Figura 03 con «Cambiar imagen».
4. Guarda el `.txt` modificado en el repositorio.

---

## E2. Capturas de pantalla nuevas

Las capturas actuales son del **12 de octubre de 2026** y no muestran las funciones del día 1. Hay que rehacer algunas figuras de la sección 3.8.3.

### E2.0 Preparación (una sola vez)

1. Arranca la aplicación con los datos de demostración, desde `backend/`:
   ```bash
   mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev,demo
   ```
   Las cuentas (consumidor, empresa, trabajador y administrador) están en el `README.md`, en la sección de la demo. Usa esas, no crees cuentas nuevas.
2. Abre `http://localhost:8080/` en **Chrome o Edge**.
3. **Para el portal del consumidor (formato móvil):** F12 → botón de dispositivo (Ctrl+Shift+M) → tamaño **390 × 844**. Es el mismo formato de las figuras actuales.
4. **Para empresa, trabajador y administración:** ventana de escritorio. Igual que en las figuras actuales, oculta la barra lateral para ampliar el contenido.
5. **Ubicación simulada (para «Cerca de mí»):** F12 → menú ⋮ → *More tools* → **Sensors** → *Location* → *Other…* y escribe, por ejemplo, latitud `-12.1190` y longitud `-77.0350` (cerca de Miraflores). Cuando la página pida permiso, acepta. Las coordenadas de los locales demo son: Aurora Miraflores `-12.1219, -77.0297`; Aurora San Borja `-12.1002, -76.9990`; los dos locales de Ate `-12.0409, -76.9582` y `-12.0390, -76.9305`.
6. **Consola H2** (para adelantar fechas): `http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:sazloop`.
7. Para capturar: Ctrl+Shift+P en DevTools → «Capture screenshot» (en modo dispositivo saca la pantalla exacta de 390 × 844). En escritorio, la herramienta Recortes de Windows (Win+Shift+S).
8. Para reemplazar cada imagen en Word: **clic derecho → Cambiar imagen → Desde un archivo**. Así se conservan el tamaño y la posición dentro de la tabla de la figura.

### E2.1 Figura 09: portal del consumidor, búsqueda (rehacer b y c)

- **(a) inicio con novedades:** puede quedar igual.
- **(b) filtros:** inicia sesión como consumidor → *Productos* → abre los filtros. Debe verse la casilla **«Cerca de mí»** con su texto «Ordena del más cercano al más lejano. Tu ubicación no se guarda.» Captura con la casilla marcada.
- **(c) resultados:** con «Cerca de mí» activo y la ubicación simulada, aplica los filtros. Cada tarjeta debe mostrar la distancia («a 0,4 km», «a 9,8 km»…), ordenadas de la más cercana a la más lejana. Si algún local no tiene coordenadas, aparece al final con «Distancia no disponible».
- **Pie nuevo:**
  > Figura 09. Portal del consumidor: (a) inicio con las novedades, (b) filtros de búsqueda con la opción «Cerca de mí» y (c) resultados ordenados por cercanía con la distancia a cada local.

### E2.2 Figura 10: ficha, pago y confirmación (rehacer b y c)

- **(a) ficha del pack:** puede quedar igual.
- **(b) resumen y pago:** agrega un pack al carrito → *Mis compras* (carrito). Elige **«Delivery»** y llena los datos:
  - empresa: uno de los tres botones de color, por ejemplo PedidosYa;
  - distrito y dirección; la referencia es opcional.

  Deben verse la nota sobre el costo del envío y el resumen de términos con el enlace «Leer términos completos». Captura antes de pagar.
- **(c) compra confirmada:** paga con Yape (cualquier número peruano y 6 dígitos). En la boleta debe verse **«Modalidad: Delivery con PedidosYa…»** y el código de recojo.
- **Pie nuevo:**
  > Figura 10. Portal del consumidor: (a) ficha del pack con alérgenos, dieta, tamaños y precios, (b) resumen con la modalidad de entrega (recojo o delivery simulado) y pago simulado y (c) compra confirmada con boleta simulada, modalidad y código de recojo.

### E2.3 Figura 11: compra presencial, historial y calificación (opcional b)

- **(a)** y **(c)** pueden quedar igual. En `qr.html` no cambió nada.
- **(b) historial (opcional, recomendado):** con la compra de E2.2 aún sin entregar, *Mis compras* muestra la etiqueta **«Esperando al repartidor»**. Si rehaces la captura, cambia el pie:
  > … (b) historial de compras con sus estados y modalidad de entrega …

### E2.4 Figura 12: portal de empresa, resumen (rehacer)

1. Antes de capturar tiene que haber **al menos una entrega**, si no las tarjetas salen en 0,0 kg:
   - como consumidor, compra un pack (puede ser el delivery de E2.2);
   - como trabajador del mismo local → *Validar código* → escribe el código → **«Entregado al repartidor»** (o «Confirmar entrega» si es recojo).
2. Inicia sesión como la empresa dueña de ese local → *Resumen*.
3. Deben verse, junto a «Packs entregados», las dos tarjetas verdes **«Comida recuperada: X kg»** y **«CO₂ evitado: Y kg»**, con 1 decimal y coma. Por ejemplo, 2 unidades medianas dan 2,0 kg y 5,0 kg.
4. **Pie nuevo:**
   > Figura 12. Portal de empresa: resumen general de ventas, comisión, ingresos, cancelaciones, reservas no recogidas, calificación, packs entregados, comida recuperada y CO₂ evitado.

### E2.5 Figura 13: locales de la empresa (opcional)

- Hoy la figura muestra la lista de locales y los documentos. Si quieres mostrar la ubicación: empresa → *Mis locales* → *Gestionar local*. En el detalle se ve **«Ubicación registrada»** o «Sin ubicación» y el botón **«Actualizar ubicación»**. Puedes cambiar la (b) o añadir una (c):
  > … (c) ubicación registrada del local para «Cerca de mí».

### E2.6 Figura 17: trabajador, validación del código (añadir c)

- **(a)** y **(b)** pueden quedar igual.
- **(c) nueva:** como trabajador, valida el código del pedido delivery de E2.2 **antes de entregarlo**. Debe verse «Delivery: PedidosYa – dirección, distrito (referencia)» y el botón **«Entregado al repartidor»**.
- En Word: copia una de las celdas de la tabla de la figura para tener un tercer espacio y pon la etiqueta «(c)» debajo, como en las figuras de tres partes.
- **Pie nuevo:**
  > Figura 17. Portal del trabajador: validación del código de recojo: (a) pedido pagado con Yape listo para entregar, (b) pedido en efectivo pendiente de cobro y (c) pedido con delivery simulado listo para entregar al repartidor.

### E2.7 Figura 18: suspensión automática (rehacer b)

La captura actual se tomó antes de corregir el punto que faltaba entre el motivo y «Puedes validar…». Para rehacerla:

1. Consola H2. Busca un documento aprobado de «Sazón Urbano Ate»:
   ```sql
   SELECT d.id, d.tipo_documento, d.fecha_vencimiento, e.nombre
   FROM documentos_establecimiento d
   JOIN solicitudes_afiliacion s ON s.id = d.solicitud_id
   JOIN establecimientos e ON e.id = s.establecimiento_id;
   ```
   Revisa los nombres de las tablas y columnas en la lista de la izquierda de la consola si alguno no coincide.
2. Pon su vencimiento en ayer:
   ```sql
   UPDATE documentos_establecimiento SET fecha_vencimiento = DATEADD('DAY', -1, CURRENT_DATE) WHERE id = <id>;
   ```
3. Espera hasta 1 minuto (la revisión de vigencias corre cada minuto).
4. **(a)** administración → *Establecimientos* → filtro «Suspendidos»: el local aparece suspendido, con motivo y fecha.
5. **(b)** inicia sesión como el trabajador de Sazón Urbano Ate. Debe verse la banda roja «Local suspendido por documentos vencidos» con el motivo **terminado en punto** antes de «Puedes validar…», y el formulario de «Crear nuevo pack» bloqueado.
6. **(c)** empresa (dueña de Sazón Urbano) → *Notificaciones*: el aviso de suspensión.
7. El pie puede quedar igual.

### E2.8 Textos del informe que dependen de las capturas

Cuando termines E2.1 a E2.7, en el `.docx`:
- **3.8 (introducción):** «Las capturas corresponden a la aplicación ejecutada el 12 de octubre de 2026 con datos de prueba.» → cambia la fecha por la de las capturas nuevas. Si mezclas capturas viejas y nuevas: «…ejecutada el 12 de octubre y el [fecha] de 2026…».
- **Pies de figura:** usa los textos nuevos de E2.1, E2.2, E2.4, E2.6 y, si las rehiciste, E2.3 y E2.5.
- **Anexo «Adjuntar pantallazos de funcionalidad»:** si allí van capturas, agrega las mismas pantallas nuevas.

---

## E3. Nueva medición de rendimiento (fila «Rendimiento» de 3.8.6)

**Por qué:** la medición del 12 de octubre se hizo con la búsqueda anterior, que filtraba en Java y hacía unas 7 consultas por oferta. Desde el día 1, la búsqueda filtra en la base de datos con **8 consultas fijas**. La cifra «mediana de 15 ms, máximo 30 ms» ya no corresponde al código actual y hay que medir de nuevo.

Se recomienda hacer dos mediciones: **A** repite la del 12 de octubre con el mismo método, para comparar. **B** es la que el informe ya da por pendiente (MySQL y varios usuarios, sección 3.5.1).

### E3.1 Qué se mide

| Operación | Petición | Notas |
|---|---|---|
| Búsqueda con filtros | `GET /api/ofertas?distrito=Ate&precioMaximo=20&calificacionMinima=3.5` | Sin sesión. |
| Búsqueda «Cerca de mí» | `GET /api/ofertas?lat=-12.1190&lng=-77.0350` | Nueva desde el día 1; conviene medirla aparte. |
| Compra simulada | `POST /api/compras/lote` | Requiere sesión de consumidor (cookie). |

Cuerpo de la compra (cambia `ofertaId` y `varianteId` por los reales; los ves con `GET /api/ofertas`):

```json
{
  "medioPago": "YAPE",
  "pedidos": [ { "ofertaId": 1, "lineas": [ { "varianteId": 1, "cantidad": 1 } ] } ]
}
```

Para probar también el delivery, agrega `"modalidad": "DELIVERY", "empresaDelivery": "RAPPI", "entregaDistrito": "Miraflores", "entregaDireccion": "Av. Larco 100"`.

**Cuidado con estas dos cosas:**
- **Límite de intentos:** inicia sesión **una sola vez** por usuario simulado (`POST /api/sesion` con `email` y `contrasena`) y reutiliza la cookie. Si fallas 5 veces seguidas, ese correo queda bloqueado 5 minutos (429).
- **Stock:** cada compra descuenta stock. Si la oferta se queda sin unidades, las compras dan 409 y esos tiempos no sirven. Antes de medir, sube el stock en H2 o MySQL (por ejemplo, `UPDATE variantes_pack SET stock_disponible = 1000;`; revisa el nombre real de la tabla y la columna) o publica una oferta con mucho stock desde el portal del trabajador.

### E3.2 Medición A: un cliente, base H2 (comparable con la del 12 de octubre)

1. Arranca con `dev,demo` (ver E2.0).
2. Ejecuta en PowerShell. El script hace 100 búsquedas con filtros y 100 «Cerca de mí», e imprime la mediana y el máximo en milisegundos:
   ```powershell
   function Medir($url, $n) {
     $t = 1..$n | ForEach-Object { (Measure-Command { Invoke-WebRequest $url -UseBasicParsing | Out-Null }).TotalMilliseconds }
     $o = $t | Sort-Object
     "{0}: mediana {1:N1} ms, máximo {2:N1} ms ({3} repeticiones)" -f $url, $o[[int]($n/2)], $o[-1], $n
   }
   Medir "http://localhost:8080/api/ofertas?distrito=Ate&precioMaximo=20&calificacionMinima=3.5" 100
   Medir "http://localhost:8080/api/ofertas?lat=-12.1190&lng=-77.0350" 100
   ```
3. Para la compra, inicia sesión una vez y repite `POST /api/compras/lote` usando la misma sesión:
   ```powershell
   $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
   Invoke-RestMethod http://localhost:8080/api/sesion -Method Post -WebSession $s -ContentType 'application/json' -Body '{"email":"<correo del consumidor demo>","contrasena":"<su contraseña del README>"}' | Out-Null
   $body = '{"medioPago":"YAPE","pedidos":[{"ofertaId":1,"lineas":[{"varianteId":1,"cantidad":1}]}]}'
   $t = 1..50 | ForEach-Object { (Measure-Command { Invoke-RestMethod http://localhost:8080/api/compras/lote -Method Post -WebSession $s -ContentType 'application/json' -Body $body | Out-Null }).TotalMilliseconds }
   $o = $t | Sort-Object; "compra: mediana {0:N1} ms, máximo {1:N1} ms" -f $o[25], $o[-1]
   ```
   La primera petición suele ser más lenta (el servidor «se calienta»). Haz unas 5 peticiones de prueba antes de medir, o descarta la primera.
4. Anota: fecha, equipo (procesador y RAM), número de repeticiones, mediana y máximo de cada operación.

### E3.3 Medición B: MySQL y varios usuarios a la vez (recomendada)

1. Configura MySQL como dice el README («Configurar MySQL»: copiar `application-local.properties.example` y poner tu contraseña) y arranca con el perfil `mysql` desde `backend/`.
2. Como los datos demo solo se cargan en H2, en MySQL crea a mano una empresa, un local aprobado, una oferta con mucho stock y varios consumidores.
3. Usa una herramienta de carga. Con **k6** (`winget install k6`), este script simula 20 usuarios buscando durante 30 segundos:
   ```javascript
   // busqueda.js  →  k6 run busqueda.js
   import http from 'k6/http';
   export const options = { vus: 20, duration: '30s' };
   export default function () {
     http.get('http://localhost:8080/api/ofertas?distrito=Ate&precioMaximo=20');
     http.get('http://localhost:8080/api/ofertas?lat=-12.1190&lng=-77.0350');
   }
   ```
   k6 muestra `http_req_duration` con `med` (mediana), `p(95)` y `max`. Para la compra, cada usuario virtual debe iniciar sesión una vez en `setup()` o en su primera iteración, con un consumidor distinto por usuario. Así no se dispara el límite de intentos y no compiten por la misma sesión.
4. Prueba 1, 10 y 20 usuarios y anota los tres resultados.

### E3.4 Texto para el informe

Reemplaza el contenido de la celda «Rendimiento» de 3.8.6 por este modelo, con tus números:

> Medición local del **[fecha]**, con la versión que filtra la búsqueda en la base de datos (8 consultas fijas por búsqueda): con un solo cliente y la base H2 en memoria, la búsqueda con filtros (**[n]** repeticiones), la búsqueda «Cerca de mí» (**[n]**) y la compra simulada (**[n]**) respondieron con medianas de **[x] ms**, **[y] ms** y **[z] ms**, y ninguna superó los **[máx] ms**, por debajo de los 3 segundos del requerimiento. Con MySQL y **[u]** usuarios simultáneos (k6, **[duración]**), la mediana fue de **[m] ms** y el percentil 95 de **[p] ms**. Equipo: **[procesador, RAM]**.

Si no haces la medición B, quita esa oración y deja la que ya existe: «La medición no incluye usuarios simultáneos ni la base MySQL.»

Revisa también:
- **3.5.1, último párrafo:** si hiciste la medición B, cambia «deberá comprobarse posteriormente» por una referencia a 3.8.6.
- **Anexo «Adjuntar reporte de pruebas funcionales»** (o «otros»): adjunta la salida de PowerShell o de k6 como evidencia.

---

## Al terminar todo: actualizar la tabla de contenido

El texto corregido agregó párrafos (limitaciones, fundamento, definiciones, código), así que los números de página de la tabla de contenido ya no coinciden. En Word: clic sobre la tabla de contenido → **Actualizar tabla → Actualizar toda la tabla**. Haz lo mismo con el índice de figuras si existe. Revisa también que ninguna figura nueva haya quedado partida entre dos páginas.
