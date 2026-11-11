# Análisis del informe final

**Fuente única:** `docs/informes/Proyecto final/Proyecto_final.docx` (no se revisó ningún otro archivo).  
**Fecha del análisis:** 4 de noviembre de 2026 · **actualizado:** 10 de noviembre de 2026

## Cómo leer la columna «Estado»

- **Realizado:** el informe dice que ya existe o funciona (por ejemplo, en 3.7, 3.8, en las pruebas o en las capturas de pantalla).
- **Pendiente:** el informe dice que se hará más adelante, o deja algo por completar.
- **No se hará:** el informe dice claramente que queda fuera.
- **No indicado:** el informe lo menciona, pero no dice si ya está hecho.

Notas:

- Los RF-01 a RF-26 están como «Realizado» porque la sección 3.8.2 dice: «Los 26 requerimientos funcionales cuentan con backend y pantalla conectados».
- La parte «Definiciones funcionales aprobadas» (en Anexos) describe reglas, pero no dice si cada una ya está programada. Por eso, una regla de esa parte solo se marcó «Realizado» si otra sección del informe lo confirma (código, pruebas o capturas).
- Los diagramas y capturas son imágenes; se usaron sus títulos y el texto que los acompaña.

## Qué cambió en la actualización del 10 de noviembre

El informe se corrigió para que coincida con el código. Filas cambiadas o agregadas:

- **QR de cada oferta:** RF-17, RF-18 y una fila nueva sobre imprimir un QR nuevo con cada oferta.
- **Pantallas de acceso:** ahora son tres, no una por portal (fila nueva).
- **Seguridad:** dos filas nuevas sobre las credenciales de demostración.
- **Pruebas automáticas:** 14 pruebas en 7 clases, ejecutadas el 6/11/2026.
- **Prototipos:** el informe ya no menciona Google Stitch.
- **Novedades:** ya no dice que son tres ofertas.
- **Definiciones funcionales:** filas nuevas sobre el registro, la cancelación, volver a publicar un pack, las observaciones obligatorias, el documento vencido y la vista «Establecimientos»; también cambiaron la fila de la tarjeta y la del motivo de la decisión.

## Tabla

| Qué dice el informe | Sección del informe | Estado según el informe |
|---|---|---|
| La plataforma tiene cuatro tipos de usuario: consumidor, propietario de empresa, personal del local (trabajador) y administrador. | 1.3 Alcance; 3.8.2 | Realizado |
| Conectarse con bancos, billeteras digitales o pasarelas de pago reales. Los pagos son solo simulados (de prueba, no se cobra dinero real). | 1.4 Limitaciones | No se hará |
| Integración con empresas externas de delivery. Hoy solo hay recojo en el local; la integración se plantea como función futura para el avance final. | 1.4 Limitaciones; Anexos → Levantamiento de observaciones (obs. 4) | Pendiente |
| Servicio de reparto (delivery) propio de Sazloop. | 1.4 Limitaciones ("sin contemplar un servicio de reparto propio") | No se hará |
| Usar GPS, mapas interactivos o seguimiento de ubicación en tiempo real en la primera versión. La búsqueda usa texto (distrito y dirección). | 1.4 Limitaciones | No se hará |
| Investigar cómo registrar la ubicación con GPS para evaluarlo en una versión futura. | 1.4 Limitaciones; Anexos → Levantamiento de observaciones (obs. 5) | Pendiente |
| Filtro «Cerca de mí»: aparece en la pantalla de filtros como función próxima. | Anexos → Levantamiento de observaciones (obs. 5) | Pendiente |
| Revisar documentos de forma automática con OCR (programa que lee el texto de una imagen). La revisión la hace una persona (el administrador). | 1.4 Limitaciones | No se hará |
| Que la plataforma actúe como autoridad sanitaria o emita certificados propios. Solo revisa el estado y la vigencia de los documentos. | 1.4 Limitaciones | No se hará |
| Saber qué trabajador hizo cada operación. Cada local usa una sola cuenta compartida. | 1.4 Limitaciones | No se hará |
| Que un local tenga más de una oferta activa al mismo tiempo (sí puede tener varios tamaños, precios y cantidades dentro de esa oferta). | 1.4 Limitaciones; 3.1 (RF-12) | No se hará |
| Aplicaciones nativas para Android o iOS (apps que se instalan desde la tienda). Solo habrá versión web que se adapta al celular. | 1.4 Limitaciones | No se hará |
| Iniciar sesión con servicios externos como Google. | 1.4 Limitaciones ("no será funcional durante esta versión") | No se hará |
| Fórmula para calcular los kilos de alimento recuperado y el CO₂ evitado. Se definirá en una etapa posterior. | 1.4 Limitaciones; Anexos → Definiciones funcionales aprobadas (Compra presencial e impacto) | Pendiente |
| Donar alimentos a organizaciones benéficas (como hace Phenix). Sazloop se enfoca "exclusivamente" en la compra simulada. | 2.1 Antecedentes (Phenix) | No se hará |
| RF-01: Crear una cuenta con datos personales básicos. | 3.1 (Módulo de Identidad); 3.8.2 | Realizado |
| RF-02: Iniciar y cerrar sesión. | 3.1 (Módulo de Identidad); 3.8.2 | Realizado |
| RF-03: Una misma cuenta puede usarse como consumidor y como empresa. | 3.1 (Módulo de Identidad); 3.8.2 | Realizado |
| RF-04: El propietario registra los datos de su empresa. | 3.1 (Módulo de Identidad); 3.8.2 | Realizado |
| RF-05: El propietario registra uno o más locales de su empresa, cada uno con su ubicación. | 3.1 (Módulo de Identidad); 3.8.2 | Realizado |
| RF-06: El propietario adjunta los documentos obligatorios de cada local. | 3.1 (Módulo de Afiliación); 3.8.2 | Realizado |
| RF-07: El administrador anota el resultado de la revisión manual de cada documento, con observaciones. | 3.1 (Módulo de Afiliación); 3.8.2 | Realizado |
| RF-08: El administrador aprueba o rechaza la solicitud de un local e indica el motivo. | 3.1 (Módulo de Afiliación); 3.8.2 | Realizado |
| RF-09: Si vence un documento obligatorio, el local se suspende solo (automáticamente) hasta que el administrador apruebe la renovación. | 3.1 (Módulo de Afiliación); 3.7; 3.8.2; 3.8.4 | Realizado |
| RF-10: El propietario crea una cuenta operativa (la cuenta que usa el personal del local) para cada local, solo después de que esté aprobado. | 3.1 (Módulo de Identidad); 3.8.2 | Realizado |
| RF-11: El local escribe la descripción y los posibles contenidos de su pack sorpresa. | 3.1 (Módulo de Ofertas); 3.8.2 | Realizado |
| RF-12: El local publica una sola oferta activa con una o más variantes (tamaño, precio y stock). | 3.1 (Módulo de Ofertas); 3.8.2; 3.8.4 | Realizado |
| RF-13: El local define la hora de inicio y fin del recojo. La hora de fin es el único límite para entregar o cancelar. | 3.1 (Módulo de Ofertas); 3.8.2; 3.8.4 | Realizado |
| RF-14: El consumidor busca y filtra locales por distrito, dirección, precio y calificación. | 3.1 (Módulo de Ofertas); 3.8.2; 3.8.3 (Fig. 09) | Realizado |
| RF-15: El consumidor ve la información del local, la oferta, el descuento, los alérgenos y las restricciones alimentarias. | 3.1 (Módulo de Ofertas); 3.8.2; 3.8.3 (Fig. 10) | Realizado |
| RF-16: El consumidor compra una o más unidades con pago simulado por tarjeta de débito o Yape. | 3.1 (Módulo de Transacciones); 3.8.2; 3.8.4 | Realizado |
| RF-17: Al escanear el código QR de una oferta (impreso y colocado en el local), se abre directamente esa oferta activa. | 3.1 (Módulo de Ofertas); 3.8.2; 3.8.3 (Fig. 11) | Realizado |
| RF-18: El pago en efectivo solo está disponible en la compra presencial iniciada con el QR de la oferta, además de los pagos digitales simulados. | 3.1 (Módulo de Transacciones); 3.8.2; 3.8.4 | Realizado |
| RF-19: Mientras se confirma un pago en efectivo, el stock elegido queda apartado (retenido) por un tiempo. | 3.1 (Módulo de Reservas); 3.7; 3.8.2 | Realizado |
| RF-20: Después de pagar se genera una boleta simulada y un código único de recojo. | 3.1 (Módulo de Transacciones); 3.8.2; 3.8.3 (Fig. 10) | Realizado |
| RF-21: El personal del local busca el código de recojo y confirma la entrega del pack. | 3.1 (Módulo de Reservas); 3.8.2; 3.8.3 (Fig. 17) | Realizado |
| RF-22: El consumidor puede cancelar una reserva pagada a distancia, dentro del plazo, y recibe un reembolso simulado del 50 %. | 3.1 (Módulo de Reservas); 3.8.2; 3.8.5 | Realizado |
| RF-23: Después de recoger, el consumidor deja una calificación (obligatoria) y un comentario (opcional). | 3.1 (Módulo de Experiencia); 3.8.2; 3.8.3 (Fig. 11) | Realizado |
| RF-24: El propietario ve un panel con ventas, comisiones, ingresos, cancelaciones, reservas no recogidas, reseñas e impacto, por local y en total. Sazloop cobra 10 % de comisión sobre lo que queda cobrado (si hay cancelación, sobre el 50 % retenido). | 3.1 (Módulo de Experiencia); 3.8.2; 3.8.3 (Fig. 12); 3.8.4 | Realizado |
| RF-25: El sistema envía notificaciones sobre solicitudes, documentos, suspensiones y recojos. | 3.1 (Módulo de Experiencia); 3.8.2; 3.8.3 (Fig. 18) | Realizado |
| RF-26: Después de cada recojo se registra de forma conceptual (sin cálculo real) el alimento recuperado y el CO₂ evitado. | 3.1 (Módulo de Experiencia); 3.8.2 | Realizado |
| Diseño mobile-first: la web se adapta a celular y computadora (etiqueta viewport y media queries, que son reglas de estilo según el tamaño de pantalla). En portales internos el menú es horizontal en pantallas menores de 900 px y lateral desde 900 px. | 3.2; 3.8.6 | Realizado |
| Buscar ofertas y confirmar una compra simulada en 3 segundos o menos. Medición local: mediana de 15 ms, ninguna sobre 30 ms (un solo usuario y base H2). | 3.2; 3.8.6 | Realizado |
| Probar el rendimiento con varios usuarios a la vez y con la base MySQL. | 3.5.1 ("deberá comprobarse posteriormente"); 3.8.6 ("no incluye usuarios simultáneos ni la base MySQL") | Pendiente |
| Backend como monolito modular por capas: una sola aplicación dividida en módulos, y cada módulo en capas (controller, service, repository, domain/modelo y dto). | 2.2; 3.2; 3.7; 3.8.6 | Realizado |
| Tecnologías: Java 21, Spring Boot 4.1.1, Maven, Spring Data JPA e Hibernate, MySQL en el backend; HTML, CSS y JavaScript en el frontend. | 1.3; 3.2; 3.7; 3.8.6 | Realizado |
| Seguridad de acceso: cada rol (persona, cuenta del local, administrador) tiene su propia identidad en la sesión; cookie HttpOnly (no la puede leer JavaScript); responde 401 si falta la identidad; el identificador de sesión se renueva al entrar; cerrar un rol no cierra los otros. | 3.2; 3.7; 3.8.6 | Realizado |
| Contraseñas guardadas con PBKDF2, sal aleatoria y 210 000 iteraciones (se guardan "revueltas", no en texto visible). | 3.7; 3.8.4; 3.8.6 | Realizado |
| En esta versión académica, que corre en una computadora local, las credenciales del administrador y de MySQL son de demostración y están escritas en los archivos de configuración del repositorio. | 3.2 (Seguridad de acceso) | Realizado |
| En un entorno de producción, configurar esas credenciales con variables de entorno (valores que se guardan en cada computadora, fuera del proyecto). | 3.2 (Seguridad de acceso) | No indicado |
| Evitar la sobreventa: compras, cancelaciones y entregas bloquean la oferta, la reserva o el código dentro de transacciones (operaciones que se hacen completas o no se hacen). | 2.2; 3.2; 3.8.6 | Realizado |
| API REST (la forma en que las pantallas piden datos al servidor): 53 servicios web en 15 controladores. El frontend no entra directo a la base de datos. | 3.7 | Realizado |
| Seis módulos (Identidad, Afiliación, Ofertas, Reservas, Transacciones, Experiencia) más el paquete shared, que maneja las sesiones por rol. | 3.7 | Realizado |
| El módulo Transacciones no tiene controladores propios: sus operaciones se ejecutan desde los servicios de Reservas. | 3.7 | Realizado |
| 31 entidades (clases que se guardan en la base) que generan 33 tablas, incluidas 2 tablas puente: pack_alergenos y pack_restricciones. | 3.5.1; 3.5.2 | Realizado |
| Dos perfiles de base de datos: dev (H2 en memoria, para desarrollo y pruebas) y mysql (base MySQL sazloop). | 3.7; 3.8.1 | Realizado |
| Tres tareas automáticas que corren cada minuto: revisar vigencia de documentos (también al iniciar), vencer reservas no recogidas y liberar stock retenido de compras en efectivo. | 3.7 | Realizado |
| Cinco interfaces conectadas a la API: sitio público, portal del consumidor, portal del trabajador, portal de empresa y portal de administración. | 3.7; 3.8.2 | Realizado |
| Tres pantallas de acceso: la del consumidor; la de locales, compartida por el trabajador (entra con la cuenta operativa del local) y la empresa (entra con la cuenta personal del consumidor, RF-03) mediante un selector de rol; y la de administración. El sitio público no pide iniciar sesión: abre una ventana para elegir a qué pantalla ir. | 3.8.2 | Realizado |
| Código en GitHub (Zheyben/Sazloop) con carpetas backend y frontend; Maven arma un solo archivo JAR que sirve pantallas y API desde el mismo servidor (Tomcat embebido). | 3.8.1 | Realizado |
| Los comentarios de las clases principales indican qué requerimiento (RF) implementan. | 3.8.2 | Realizado |
| 14 pruebas automáticas en 7 clases (JUnit y Spring Boot Test, con H2). Resultado del 6/11/2026: 0 fallos, 0 errores, BUILD SUCCESS. | 3.8.5 | Realizado |
| El código de recojo se usa una sola vez y solo antes del fin del horario. | 3.8.4; 3.8.5 | Realizado |
| Diagrama de casos de uso, con cada caso rotulado con su RF (CU/RF-01 a CU/RF-26) y agrupado por módulo. | 3.3; Anexos → Levantamiento de observaciones (obs. 9) | Realizado |
| Diagrama de clases con 31 clases, atributos, métodos, relaciones y multiplicidades. | 3.4 | Realizado |
| Diagrama entidad-relación de 33 tablas; su definición se guarda como archivo en el repositorio (docs/diagramas). | 3.5; Anexos → Levantamiento de observaciones (obs. 11) | Realizado |
| Prototipos de pantallas (afiliación, oferta, búsqueda, pago y recojo). | 3.6 | Realizado |
| Registro: cada correo solo puede usarse en una cuenta; la contraseña debe tener al menos 8 caracteres; cada cuenta registra una sola empresa; el RUC debe tener 11 dígitos y no puede estar repetido. | Anexos → Definiciones funcionales aprobadas (Registro de cuentas y empresas) | No indicado |
| El inicio del consumidor muestra la sección «Novedades» con las ofertas recién publicadas. | Anexos → Definiciones funcionales aprobadas (Oferta y consulta de packs); 3.8.3 (Fig. 09) | Realizado |
| La ficha muestra la calificación del local, o un guion si aún no tiene reseñas. | Anexos → Definiciones funcionales aprobadas (Oferta y consulta de packs) | No indicado |
| Al abrir una oferta las cantidades empiezan en cero, y al agregarla al carrito la ficha se cierra. | Anexos → Definiciones funcionales aprobadas (Oferta y consulta de packs) | No indicado |
| Solo se muestran los alérgenos que el local marcó. | Anexos → Definiciones funcionales aprobadas (Oferta y consulta de packs) | No indicado |
| La información alimentaria siempre muestra tres opciones: vegetariano, vegano y celiaquía. Si una no está marcada, se muestra como no apta (también en packs antiguos sin esa información). | Anexos → Definiciones funcionales aprobadas (Oferta y consulta de packs) | No indicado |
| Cada oferta tiene su propio código QR, que la abre mientras siga disponible. | Anexos → Definiciones funcionales aprobadas (Oferta y consulta de packs; Compra presencial e impacto); 3.8.3 (Fig. 11) | Realizado |
| Como el QR cambia con cada oferta, el local debe imprimir y colocar un QR nuevo cada vez que publica; el QR de una oferta terminada ya no permite comprar. | Anexos → Definiciones funcionales aprobadas (Oferta y consulta de packs) | No indicado |
| El carrito acepta pedidos separados, incluso del mismo pack, y su contador muestra la cantidad de pedidos (no de unidades). | Anexos → Definiciones funcionales aprobadas (Carrito y pagos) | No indicado |
| Todo el carrito se paga en una sola operación; cada pedido tiene su propia reserva, boleta y código. Si un pedido se queda sin stock, no se confirma nada. | Anexos → Definiciones funcionales aprobadas (Carrito y pagos); 3.8.4 | Realizado |
| Yape simulado: se pide un número peruano y luego seis dígitos; se acepta cualquier combinación y no se consulta a Yape. | Anexos → Definiciones funcionales aprobadas (Carrito y pagos: "En este prototipo se acepta…") | Realizado |
| Tarjeta simulada: se validan número, titular, una fecha de vencimiento que no sea anterior al mes actual y CVV/CVC; los datos se borran tras validar y no se envían ni guardan en el servidor. | Anexos → Definiciones funcionales aprobadas (Carrito y pagos) | No indicado |
| Antes de pagar hay que aceptar los términos; el texto de términos es provisional. | Anexos → Definiciones funcionales aprobadas (Carrito y pagos) | No indicado |
| Reglas de cancelación: solo se cancelan las compras remotas (hechas desde el portal, no las presenciales con QR) y antes del fin del horario de recojo. Al cancelar, el reembolso simulado es del 50 % y el código de recojo deja de valer. Las unidades vuelven a estar disponibles si la oferta sigue activa y el local no está suspendido. | Anexos → Definiciones funcionales aprobadas (Cancelación de compras) | No indicado |
| Al volver a entrar, el consumidor recibe un aviso para reseñar cada local donde recogió sin reseñar; si lo cierra, renuncia a esa reseña, pero una nueva compra recogida puede volver a activarlo. | Anexos → Definiciones funcionales aprobadas (Reseñas y perfil) | No indicado |
| Solo se publica una reseña por consumidor y por local. | Anexos → Definiciones funcionales aprobadas (Reseñas y perfil) | No indicado |
| El consumidor ve y edita en su perfil nombres, apellidos, correo y teléfono. | Anexos → Definiciones funcionales aprobadas (Reseñas y perfil); 3.8.5 (prueba de actualización del perfil) | Realizado |
| Cambiar la contraseña en un formulario aparte que pide la contraseña actual. | Anexos → Definiciones funcionales aprobadas (Reseñas y perfil) | No indicado |
| Subir o cambiar la foto de perfil. | Anexos → Definiciones funcionales aprobadas (Reseñas y perfil) | No indicado |
| El trabajador ve la oferta vigente (solo lectura), valida códigos de recojo y crea un pack con un formulario por pasos (contenido, tamaños y precios, horario). | Anexos → Definiciones funcionales aprobadas (Cuenta operativa del establecimiento); 3.8.3 (Figs. 15, 16 y 17) | Realizado |
| El formulario del pack no muestra identificadores internos ni un campo extra de límite de horario. | Anexos → Definiciones funcionales aprobadas (Cuenta operativa del establecimiento) | No indicado |
| En el primer paso del formulario, el trabajador elige entre un pack nuevo o volver a publicar uno ya registrado por su local: se reutilizan la descripción, los alérgenos y la dieta, y se definen de nuevo tamaños, precios, stock y horario. La opción se habilita si el local tiene al menos un pack. | Anexos → Definiciones funcionales aprobadas (Cuenta operativa del establecimiento) | No indicado |
| El panel del trabajador muestra el stock disponible y las ganancias netas (ya descontado el 10 %) por día, semana y mes. | Anexos → Definiciones funcionales aprobadas (Cuenta operativa del establecimiento); 3.8.3 (Fig. 15) | Realizado |
| Reglas de ese panel: semana de lunes a domingo, mes calendario, hora de Perú; el stock no vendido suma lo que sobró al cerrar cada oferta. | Anexos → Definiciones funcionales aprobadas (Cuenta operativa del establecimiento) | No indicado |
| Pago en efectivo por QR: las unidades se apartan 10 minutos; el consumidor recibe su código al reservar; el trabajador confirma el cobro con ese código. Si no se confirma a tiempo, el stock vuelve a estar disponible. Al confirmar, se completan pago y recojo y se genera la boleta. | Anexos → Definiciones funcionales aprobadas (Compra presencial e impacto); 3.7; 3.8.2; 3.8.5; 3.8.3 (Fig. 17) | Realizado |
| En esta etapa el impacto se muestra como número de packs entregados, sin kilos ni CO₂. | Anexos → Definiciones funcionales aprobadas (Compra presencial e impacto); 3.8.2 (RF-26); 3.8.3 (Fig. 12) | Realizado |
| Reglas del panel de empresa: "ventas" cuenta las reservas pagadas aunque no se hayan recogido; las canceladas no cuentan como ventas y se muestran aparte, pero su importe retenido y comisión sí entran en ingresos y comisiones. | Anexos → Definiciones funcionales aprobadas (Ventas y reservas no recogidas) | No indicado |
| Si una reserva pagada no se entrega antes del fin del horario, pasa sola a "no recogida"; se conservan el cobro y la comisión, y no cuenta como pack entregado. | Anexos → Definiciones funcionales aprobadas (Ventas y reservas no recogidas); 3.7; 3.8.5 | Realizado |
| Al adjuntar un documento se indica su tipo (licencia, certificado sanitario, inspección u otro) y opcionalmente número y fechas. No hay lista cerrada de tipos; se exige al menos un documento para enviar la solicitud. | Anexos → Definiciones funcionales aprobadas (Documentos y revisión de la afiliación) | No indicado |
| El administrador resuelve la solicitud en una sola operación: se aprueba solo si todos los documentos se aprueban. | Anexos → Definiciones funcionales aprobadas (Documentos y revisión de la afiliación); 3.8.5 | Realizado |
| Para rechazar un documento es obligatorio escribir sus observaciones. | Anexos → Definiciones funcionales aprobadas (Documentos y revisión de la afiliación) | No indicado |
| Un documento vencido solo puede rechazarse. | Anexos → Definiciones funcionales aprobadas (Documentos y revisión de la afiliación); 3.8.5 (VigenciaYAvisosTests) | Realizado |
| El motivo de la decisión es un comentario general opcional; si el administrador no lo escribe, el sistema lo genera: «Documentación revisada y conforme» al aprobar, o las observaciones de los documentos rechazados al rechazar. | Anexos → Definiciones funcionales aprobadas (Documentos y revisión de la afiliación) | No indicado |
| El administrador consulta en la vista «Establecimientos» todos los locales registrados, con su empresa, RUC, distrito y estado; en los suspendidos se muestran el motivo, la fecha de inicio y si la suspensión fue automática. | Anexos → Definiciones funcionales aprobadas (Documentos y revisión de la afiliación); 3.8.2 (RF-09); 3.8.3 (Fig. 18) | Realizado |
| Esa lista se puede filtrar por locales activos, suspendidos o pendientes de aprobación, indica cuándo un local tiene una solicitud en revisión y es solo de consulta (no permite hacer cambios). | Anexos → Definiciones funcionales aprobadas (Documentos y revisión de la afiliación) | No indicado |
| El propietario recibe un aviso antes de que venza un documento aprobado (la regla dice 7 días antes; un documento sin fecha de vencimiento no vence). | Anexos → Definiciones funcionales aprobadas (Vigencia, renovación y notificaciones); 3.8.4 | Realizado |
| Un local suspendido no puede publicar ofertas. | Anexos → Definiciones funcionales aprobadas (Vigencia, renovación y notificaciones); 3.8.4 | Realizado |
| Un local suspendido no aparece en la búsqueda ni vende, pero su cuenta puede validar códigos y entregar reservas ya pagadas. | Anexos → Definiciones funcionales aprobadas (Vigencia, renovación y notificaciones) | No indicado |
| Para reactivar un local suspendido se renuevan solo los documentos vencidos (o por vencer en 7 días); el administrador lo revisa y, al aprobar, el local vuelve con su cuenta, reservas e historial. | Anexos → Definiciones funcionales aprobadas (Vigencia, renovación y notificaciones); 3.8.5; 3.8.3 (Fig. 13) | Realizado |
| El consumidor recibe un aviso cuando cancela y se le reembolsa. | Anexos → Definiciones funcionales aprobadas (Vigencia, renovación y notificaciones); 3.8.5 | Realizado |
| El consumidor recibe avisos de pedido no recogido y de reserva en efectivo vencida. | Anexos → Definiciones funcionales aprobadas (Vigencia, renovación y notificaciones) | No indicado |
| Reporte de evaluación estática de código (el anexo solo tiene el título «Adjuntar…», sin contenido). | Anexos | Pendiente |
| Reporte de pruebas funcionales (el anexo solo tiene el título «Adjuntar…», sin contenido). | Anexos | Pendiente |
| Reporte de pruebas de penetración (pruebas de seguridad; el anexo solo tiene el título «Adjuntar…», sin contenido). | Anexos | Pendiente |

## Resumen

| Estado | Filas |
|---|---|
| Realizado | 68 |
| Pendiente | 8 |
| No se hará | 10 |
| No indicado | 25 |
| **Total** | **111** |
