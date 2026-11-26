# Resumen: SonarQube y pruebas de Sazloop

**Para:** Emanuel (líder del Grupo 3)
**Fecha:** 26 de noviembre de 2026
**Ya está en GitHub:** rama `main`, commit `77c1595`

---

## En una frase

**El backend y el frontend ya no tienen ninguna observación en SonarQube, todo está en A, y la cobertura dejó de estar en 0 %.**

---

## ¿Qué pidió el profesor y qué se hizo?

| Lo que pidió el profesor | ¿Hecho? | Qué se hizo |
|---|---|---|
| Levantar las observaciones encontradas | ✅ Sí | Se corrigieron todas. Quedan **0 observaciones** en backend y frontend. |
| Completar las pruebas unitarias | ✅ Sí | Se agregaron **36 pruebas nuevas** (16 en el backend y 20 en el frontend). |
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

### Frontend («GRUPO 3 FRONT»)

| | Antes | Ahora |
|---|---|---|
| Seguridad | **B** (8 problemas) | **A** (0) ✅ |
| Fiabilidad | **C** (2 problemas) | **A** (0) ✅ |
| Mantenibilidad | A (58 problemas) | **A** (0) ✅ |
| Cobertura | 🔴 **0 %** | 🟡 **14,5 %** ⬆ |
| Duplicados | 0 % | 0 % |

---

## ¿Por qué la cobertura salía en 0 %?

El backend **sí tenía pruebas**, pero SonarQube no podía verlas. Faltaba una herramienta llamada **JaCoCo**, que mide qué partes del código recorren las pruebas. Ya está agregada, y por eso el backend pasó de **0 % a 77,1 %**.

El frontend **no tenía ninguna prueba**. Ahora tiene sus primeras 20, y pasó de **0 % a 14,5 %**.

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
- **20 pruebas nuevas** de las funciones que usan todas las páginas: precios, avisos, conexión con el servidor y vigencia de la tarjeta. Todas pasan.

**Lo que NO cambió:** la app se ve y funciona igual. Se probaron los 4 portales (consumidor, empresa, trabajador y administrador) y una compra completa, desde comprar hasta entregar con el código, y todo funciona.

---

## Lo único que sale en rojo (por si el profe pregunta)

En la tarjeta del **frontend**, el «Umbral de calidad» dice **Failed**.

**No es un error.** SonarQube pide que el código **recién cambiado** tenga 80 % de pruebas. Las 51 líneas arregladas están en las pantallas (botones y formularios), y esas solo se pueden probar simulando un navegador, cosa que todavía no tenemos. Seguridad, fiabilidad y mantenibilidad están todas en **A**.

---

## Lo que falta hacer

1. **Tomar la captura** de las dos tarjetas de SonarQube y pegarla en el informe final, como pidió el profesor.
2. **Mejorar los diagramas** del informe: ponerlos en una carpeta del repositorio en buen tamaño.
3. **Preparar la exposición.**
4. *(Opcional)* Agregar pruebas de las pantallas del frontend, para que el «Umbral de calidad» del frontend también salga en verde.

---

## Qué tiene que hacer cada uno del grupo

**Antes de seguir trabajando, actualicen su copia del proyecto** (hacer *pull* de `main`). Si no, les pueden salir conflictos.

---

## ¿Dónde está el detalle completo?

En el repositorio: `docs/informes/Proyecto final/auditoria/INFORME_CALIDAD_Y_PRUEBAS_2026-11-26.md`

Ahí está todo: cada problema corregido, cada prueba, los resultados de las pruebas en el navegador y los comandos para repetir el análisis.
