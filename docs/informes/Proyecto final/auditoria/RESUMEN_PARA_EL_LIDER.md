# Resumen: SonarQube y pruebas de Sazloop

**Para:** Emanuel (líder del Grupo 3)
**Fecha:** 26 de noviembre de 2026 (actualizado el 27 de noviembre de 2026)

---

## En una frase

**El backend y el frontend ya no tienen ninguna observación en SonarQube, todo está en A, los dos pasan el umbral de calidad (verde) y la cobertura subió de 0 % a 77 % en el backend y a 98 % en el frontend.**

---

## ¿Qué pidió el profesor y qué se hizo?

| Lo que pidió el profesor | ¿Hecho? | Qué se hizo |
|---|---|---|
| Levantar las observaciones encontradas | ✅ Sí | Se corrigieron todas. Quedan **0 observaciones** en backend y frontend. |
| Completar las pruebas unitarias | ✅ Sí | Se agregaron **324 pruebas nuevas**: 16 en el backend y 308 en el frontend, que antes no tenía ninguna. |
| Aplicar herramientas de análisis de calidad y pruebas | ✅ Sí | SonarQube y JaCoCo quedaron configurados dentro del proyecto. |
| Captura de SonarQube en el informe (backend y frontend) | ⏳ Falta | Hay que tomar la captura de las dos tarjetas y pegarla en el informe. |
| Mejorar los diagramas y la exposición | ⏳ Falta | No se tocó en este trabajo. |

---

## Antes y después (lo que sale en las tarjetas de SonarQube)

### Backend («GRUPO 3»)

| | Antes | Ahora |
|---|---|---|
| Seguridad | A | **A** |
| Fiabilidad | **C** (46 problemas) | **A** (0) ✅ |
| Mantenibilidad | A (59 problemas) | **A** (0) ✅ |
| Cobertura | 🔴 **0 %** | 🟢 **77,1 %** ✅ |
| Duplicados | 0,4 % | 0,3 % |
| Umbral de calidad | — | 🟢 **Pasa** ✅ |

### Frontend («GRUPO 3 FRONT»)

| | Antes | Ahora |
|---|---|---|
| Seguridad | **B** (8 problemas) | **A** (0) ✅ |
| Fiabilidad | **C** (2 problemas) | **A** (0) ✅ |
| Mantenibilidad | A (58 problemas) | **A** (0) ✅ |
| Cobertura | 🔴 **0 %** | 🟢 **98,3 %** ✅ |
| Duplicados | 0 % | 0 % |
| Umbral de calidad | — | 🟢 **Pasa** ✅ |

---

## ¿Por qué la cobertura salía en 0 %?

El backend **sí tenía pruebas**, pero SonarQube no podía verlas. Faltaba una herramienta llamada **JaCoCo**, que mide qué partes del código recorren las pruebas. Ya está agregada, y por eso el backend pasó de **0 % a 77,1 %**.

El frontend **no tenía ninguna prueba**. Ahora tiene **308**, que prueban **todas las pantallas**, y pasó de **0 % a 98,3 %**.

---

## ¿Qué se cambió en el código?

**Backend**
- Se arreglaron los 2 problemas que quedaban después de tu corrección: una búsqueda de texto que podía volverse lenta y una prueba que comparaba horas sin zona horaria.
- **16 pruebas nuevas** de:
  - el bloqueo después de 5 intentos fallidos de inicio de sesión;
  - el cálculo de kilos de comida y CO₂ evitado;
  - la distancia de «Cerca de mí».
- Ahora hay **32 pruebas y todas pasan**.

**Frontend**
- Se arreglaron **51 avisos** del mismo tipo: llamadas que se lanzaban sin esperar su respuesta. En la práctica, el carrito ahora muestra un mensaje si no puede cargar, en vez de quedarse en blanco.
- **308 pruebas nuevas**, todas pasan:
  - 20 de las funciones que usan todas las páginas: precios, avisos, conexión con el servidor y vigencia de la tarjeta.
  - 288 de **todas las pantallas**, con un «navegador simulado» (jsdom). Cada prueba abre la página real, hace clics y llena formularios como una persona, y revisa lo que aparece. Se probaron los ingresos, el catálogo y sus filtros, el carrito y el pago, el QR, las reseñas, el perfil, y los portales de empresa, trabajador y administrador.
  - Para esto **no se cambió nada de la app**: las pruebas usan el mismo código que ve el usuario.

**Lo que NO cambió:** la app se ve y funciona igual. Se probaron los 4 portales (consumidor, empresa, trabajador y administrador) y una compra completa, desde comprar hasta entregar con el código, y todo funciona.

---

## ¿Algo en rojo?

**No.** Los dos proyectos pasan el «Umbral de calidad» de SonarQube (sale **Passed**, en verde).

---

## Lo que falta hacer

1. **Tomar la captura** de las dos tarjetas de SonarQube y pegarla en el informe final, como pidió el profesor.
2. **Mejorar los diagramas** del informe: ponerlos en una carpeta del repositorio en buen tamaño.
3. **Preparar la exposición.**

---

## Qué tiene que hacer cada uno del grupo

**Antes de seguir trabajando, actualicen su copia del proyecto** (hacer *pull* de `main`). Si no, les pueden salir conflictos.

Para correr las pruebas del frontend en su PC, la primera vez hay que ejecutar `npm install` dentro de la carpeta `frontend`. Eso instala el navegador simulado, solo para las pruebas.

---

## ¿Dónde está el detalle completo?

En el repositorio: `docs/informes/Proyecto final/auditoria/INFORME_CALIDAD_Y_PRUEBAS_2026-11-26.md`

Ahí está todo: cada problema corregido, cada prueba, los resultados de las pruebas en el navegador y los comandos para repetir el análisis.
