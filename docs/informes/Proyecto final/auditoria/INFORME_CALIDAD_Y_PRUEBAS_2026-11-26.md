# Informe de calidad y pruebas: frontend y backend

**Proyecto:** Sazloop (Grupo 3)
**Fecha:** 26 de noviembre de 2026
**Rama:** `calidad/sonarqube-pruebas` (sale de `main` al día con GitHub; último cambio de `main`: `019122a`, «Corrección a la par de la auditoría de SC»).
**Herramientas:** SonarQube Community Build 26.11 (local, en Docker), JaCoCo 0.8.13 (cobertura del backend), JUnit 5 + Mockito + Spring Boot Test (pruebas del backend), ejecutor de pruebas de Node 24 (pruebas y cobertura del frontend), navegador (pruebas de funcionamiento).

Este informe responde a las observaciones del profesor sobre el código: **levantar las observaciones encontradas**, **completar las pruebas unitarias** y **aplicar herramientas de análisis de calidad y pruebas al proyecto**.

---

## 1. Resultado final

| | Backend («GRUPO 3») | Frontend («GRUPO 3 FRONT») |
|---|---|---|
| Seguridad | **A** (0) | **A** (0) |
| Fiabilidad | **A** (0) | **A** (0) |
| Mantenibilidad | **A** (0) | **A** (0) |
| Hotspots de seguridad | 0 | 0 |
| **Cobertura** | **77,1 %** | **14,5 %** |
| Duplicados | 0,3 % | 0,0 % |
| Pruebas automáticas | **32** (todas pasan) | **20** (todas pasan) |
| Líneas de código | 5.190 | 5.019 |
| Umbral de calidad | **Pasa** | No pasa (ver 4.3) |

**En resumen:** los dos proyectos tienen **cero observaciones** y **A en todas las calificaciones**. La cobertura, que estaba en **0 %** en los dos, ahora es **77,1 %** en el backend y **14,5 %** en el frontend.

### Capturas de SonarQube

> Pegar aquí las dos tarjetas de SonarQube (página *Projects* en http://localhost:9000/projects), igual que las que envió el profesor:
>
> `![Tarjetas de SonarQube](capturas/sonarqube-proyectos.png)`

---

## 2. Antes y después

«Antes» es el análisis que envió el profesor (informes `g3-full-issues-breakdown` y `g3front-full-issues-breakdown`, del 20 de octubre de 2026). «Después» es el análisis de esta rama.

| Indicador | Backend antes | Backend después | Frontend antes | Frontend después |
|---|---|---|---|---|
| Seguridad | A (0) | A (0) | B (8) | **A (0)** |
| Fiabilidad | C (46) | **A (0)** | C (2) | **A (0)** |
| Mantenibilidad | A (59) | **A (0)** | A (58) | **A (0)** |
| Cobertura | 0,0 % | **77,1 %** | 0,0 % | **14,5 %** |
| Duplicados | 0,4 % | 0,3 % | 0,0 % | 0,0 % |

**Cómo se llegó aquí, paso a paso:**

| Paso | Backend | Frontend |
|---|---|---|
| Análisis del profesor (20/10) | 105 observaciones, 0 % | 68 observaciones, 0 % |
| Corrección de la auditoría (commit `019122a`) y primer análisis de esta rama | 2 observaciones, 75,7 % (16 pruebas) | 51 observaciones, 0 % |
| Correcciones de esta rama | **0 observaciones, 77,1 % (32 pruebas)** | **0 observaciones, 14,5 % (20 pruebas)** |

**Aviso:** el análisis del profesor se hizo con otro servidor y otra versión de SonarQube. Algunas reglas cambian entre versiones, así que los números de «antes» no son 100 % comparables uno a uno con los de «después».

---

## 3. Backend

### 3.1 Por qué la cobertura estaba en 0 %

El proyecto **ya tenía pruebas**, pero SonarQube no podía medirlas porque faltaba **JaCoCo**, la herramienta que registra qué líneas recorren las pruebas. Se agregó en `backend/pom.xml`, junto con la configuración de SonarQube (`sonar.projectKey`, `sonar.host.url` y la ruta del informe de JaCoCo).

### 3.2 Observaciones corregidas (2)

| # | Regla | Archivo | Problema | Corrección |
|---|---|---|---|---|
| 1 | `java:S8786` | `afiliacion/service/RevisionAfiliacionService.java` | La expresión regular `[.\s]+$` de `sinPuntuacionFinal()` podía ser lenta con textos especiales (*backtracking*). | Se reemplazó por un bucle que recorre el texto desde el final y quita puntos y espacios. Mismo resultado, sin riesgo de lentitud. |
| 2 | `java:S8700` | `src/test/.../TiempoYRegistrosTests.java` | Calculaba una duración entre dos `LocalDateTime`, que no tienen zona horaria. | Ahora compara dos `ZonedDateTime` en la zona `America/Lima`. |

### 3.3 Pruebas unitarias nuevas (16)

| Archivo nuevo | Pruebas | Qué revisa |
|---|---|---|
| `shared/LimiteIntentosLoginTests` | 7 | Bloqueo tras N fallos (429), mensaje en singular y plural, mayúsculas y espacios del correo, reinicio del contador tras un ingreso correcto, contador separado por tipo de acceso, valores de configuración inválidos |
| `experiencia/service/ImpactoServiceTests` | 4 | Fórmula de impacto ambiental (RF-26): 2 packs medianos = 2 kg y 5 kg de CO₂; suma de varios tamaños (4,5 kg y 11,25 kg); tamaño desconocido = 1 kg; reserva vacía = 0 |
| `ofertas/service/DistanciaHaversineTests` | 5 | «Cerca de mí»: el mismo punto está a 0 km, Miraflores–San Borja ≈ 4 km, redondeo a 2 decimales, sin ubicación del consumidor o del local no hay distancia |

### 3.4 Todas las pruebas del backend (32)

Comando: `mvnw.cmd clean verify` con el perfil `dev` (H2). **Resultado: 32 pruebas, 0 fallos, 0 errores.**

| Archivo de pruebas | Pruebas | Qué revisa |
|---|---|---|
| `SazloopApplicationTests` | 1 | Que la aplicación arranque |
| `DatosDemoH2Tests` | 1 | Que los datos de demostración se carguen bien |
| `FlujoAfp2Tests` | 3 | Flujo de afiliación de locales |
| `PortalEmpresaEtapasTests` | 1 | Etapas del portal de empresa |
| `ResolucionYSesionesTests` | 3 | Resolución de solicitudes e inicios de sesión |
| `TiempoYRegistrosTests` | 2 | Hora de Lima y registros |
| `VigenciaYAvisosTests` | 4 | Vencimiento de documentos y avisos |
| `afiliacion/SolicitudAfiliacionServiceTests` | 1 | Servicio de solicitudes de afiliación |
| `shared/LimiteIntentosLoginTests` *(nuevo)* | 7 | Límite de intentos de inicio de sesión |
| `experiencia/service/ImpactoServiceTests` *(nuevo)* | 4 | Impacto ambiental |
| `ofertas/service/DistanciaHaversineTests` *(nuevo)* | 5 | Distancia «Cerca de mí» |

### 3.5 Cobertura (JaCoCo)

| Medida | Antes de las pruebas nuevas | Después |
|---|---|---|
| Líneas | 83,5 % | **84,4 %** |
| Ramas (`if`, `switch`, etc.) | 58,9 % | **61,4 %** |
| Total que muestra SonarQube | 75,7 % | **77,1 %** |

El informe detallado se genera en `backend/target/site/jacoco/index.html` al correr las pruebas.

---

## 4. Frontend

### 4.1 Observaciones corregidas (51, de la misma regla)

**Regla `javascript:S9383`:** *«Las promesas deben esperarse con `await`, terminar en `.catch`, o marcarse como ignoradas con `void`»*.

En palabras simples: el código lanzaba funciones asíncronas (avisos en pantalla o funciones que piden datos al servidor) sin esperar su resultado y sin decir si eso era a propósito.

**Cómo se corrigió:**
- Se revisó cada una de las 51 llamadas. En 50 casos la función **ya atrapa sus propios errores** (tiene su `try/catch`) o es un aviso de SweetAlert2 que no falla. Esas se marcaron con `void`, que es la forma estándar de decir «se lanza a propósito sin esperar». El comportamiento no cambia.
- En `app/compras.js`, `mostrarCarrito()` **no atrapaba sus errores**. Se le agregó `.catch(...)`, que ahora muestra el aviso «No se pudo mostrar el carrito» si algo falla, en vez de dejar la pantalla sin mensaje.

| Archivo | Correcciones |
|---|---|
| `app/accesos.js`, `app/compras.js` | 9 cada uno |
| `app/ofertas-portal.js` | 7 |
| `app/qr.js` | 5 |
| `app/trabajador.js` | 4 |
| `app/panel.js`, `app/perfil.js`, `app/resenas-portal.js` | 3 cada uno |
| `app/comun.js`, `app/empresa.js`, `app/portal-main.js` | 2 cada uno |
| `app/notificaciones.js`, `app/portal.js` | 1 cada uno |

### 4.2 Pruebas unitarias nuevas del frontend (20)

El frontend no tenía pruebas. Se agregó `frontend/tests/comun.test.js` con **20 pruebas** de `app/comun.js`, el archivo de funciones compartidas por todas las páginas. Usan el ejecutor de pruebas que trae Node, así que **no hay que instalar nada**.

Qué revisan: formato de precios, punto final de los mensajes, texto del delivery, vigencia de la tarjeta (acepta el mes actual y rechaza el anterior), detección de falta de internet, creación de elementos sin `innerHTML`, avisos de SweetAlert2 (error, advertencia, éxito, confirmación y el caso sin SweetAlert2), y las llamadas al backend (cookie de sesión, respuesta 204, mensajes de negocio, traducción de errores técnicos a texto claro y errores 5xx).

**Resultado:** 20 pruebas, 0 fallos. `app/comun.js` queda con **100 % de líneas** y **97,6 % de ramas** cubiertas.

### 4.3 Por qué el umbral de calidad del frontend no pasa

El umbral de SonarQube («Sonar way») pide **80 % de cobertura en el código nuevo**. Las 51 líneas corregidas cuentan como código nuevo, y casi todas están dentro de las páginas (botones, formularios, pantallas), que solo se pueden ejecutar en un navegador. Las pruebas unitarias actuales cubren `comun.js`, pero no esas pantallas, así que la cobertura del código nuevo queda en 5,8 %.

**No es un error del código:** seguridad, fiabilidad y mantenibilidad están en A y no hay ninguna observación abierta. Para que el umbral pase, haría falta agregar pruebas de las pantallas con un navegador simulado (por ejemplo, jsdom o Playwright).

---

## 5. Pruebas de funcionamiento (en el navegador)

Hechas con el perfil `dev,demo` (H2 con datos de demostración) en http://localhost:8080, **antes y después** de las correcciones.

### 5.1 Ingreso a cada portal

| Portal | Cuenta de demo | Resultado |
|---|---|---|
| Consumidor | `demo.consumidor1@sazloop.test` | ✅ Entra a «Hola, Valeria». Abren bien Inicio, Productos (3 packs), Mis compras, Notificaciones y Mi perfil. |
| Empresa | `duena.aurora@sazloop.test` | ✅ Portal de empresa «Aurora Cocina»: resumen, comisión, cancelaciones, packs entregados, **comida recuperada (kg)** y **CO₂ evitado**. Muestra «1 local espera aprobación». |
| Trabajador | `demo.miraflores` | ✅ Portal del local «Aurora Miraflores» con el pack publicado, horario, dieta y stock. |
| Administrador | `demo.admin@sazloop.test` | ✅ «Revisión de locales» con la solicitud pendiente de Aurora San Borja. |

En ninguna pantalla apareció «null», «undefined» ni «NaN», y la consola del navegador no mostró errores.

### 5.2 Flujo principal: compra y entrega

| Paso | Resultado esperado | Resultado |
|---|---|---|
| Consumidor (Diego) compra 1 pack mediano de Aurora Miraflores con Yape | Compra creada (201), estado CONFIRMADA, código de recojo y boleta simulada | ✅ Total S/ 14,00; código de recojo generado |
| Trabajador consulta el código **en minúsculas y sin guion** | El sistema lo encuentra igual | ✅ 200, CONFIRMADA |
| Trabajador confirma la entrega | Estado RECOGIDA | ✅ 200, RECOGIDA |
| Trabajador intenta confirmar **el mismo código otra vez** | Lo rechaza | ✅ 409, «El código ya fue usado o la compra no está confirmada» |
| Trabajador consulta un código que no existe | No encontrado | ✅ 404 |
| Consumidor revisa «Mis compras» | Compra recogida y reseña pendiente | ✅ RECOGIDA, reseña pendiente |

### 5.3 Avisos y carrito después de las correcciones del frontend

| Prueba | Esperado | Resultado |
|---|---|---|
| Ingresar con contraseña equivocada | Aviso «No pudimos iniciar sesión» | ✅ Sale el aviso |
| «Añadir al carrito» sin elegir cantidad | Aviso «Elige al menos una unidad válida» | ✅ Sale el aviso |
| Añadir 1 pack mediano al carrito | Aviso «Añadido al carrito» | ✅ Sale el aviso |
| Abrir «Mis compras» con un pack en el carrito | El carrito muestra el pedido, el total y las opciones de pago | ✅ S/ 18,00, recojo o delivery |

### 5.4 Seguridad básica

| Prueba | Esperado | Resultado |
|---|---|---|
| Inicio de sesión con contraseña equivocada | 401 | ✅ 401 |
| Pedir datos del administrador sin iniciar sesión | 401 | ✅ 401 |

---

## 6. Pendientes

1. **Pegar las capturas** de las tarjetas de SonarQube en este informe y en el informe final, como pidió el profesor.
2. **Pruebas de las pantallas del frontend** con un navegador simulado (jsdom o Playwright), para que el umbral de calidad del frontend pase (sección 4.3).
3. **Subir la cobertura de ramas del backend** (61,4 %): agregar pruebas de los casos de error (datos inválidos, permisos, códigos repetidos).

---

## 7. Cómo repetir este análisis

Requisitos: Docker Desktop con SonarQube encendido (`docker start sonarqube`), Java 21, Node 22 o superior y un token de SonarQube (*My Account → Security*) guardado en la variable de entorno `SONAR_TOKEN`.

**Backend** (desde `backend/`, con el perfil `dev` para no escribir en MySQL):

```bat
set SPRING_PROFILES_ACTIVE=dev
mvnw.cmd clean verify sonar:sonar
```

**Frontend** (desde `frontend/`):

```bat
npm run test:cobertura
docker run --rm -e SONAR_TOKEN=%SONAR_TOKEN% -e SONAR_HOST_URL=http://host.docker.internal:9000 -v "%cd%:/usr/src" sonarsource/sonar-scanner-cli
```

La configuración está en `backend/pom.xml` (propiedades `sonar.*` y plugin JaCoCo), `frontend/sonar-project.properties` y `frontend/package.json`.
