# Comparación: informe vs. código

**Fuentes:** `docs/analisis/1-informe.md` (lo que dice el informe) y `docs/analisis/2-codigo.md` (lo que se encontró en el código). No se revisó ningún otro archivo.  
**Fecha:** 4 de noviembre de 2026  
**Todavía no se cambió nada.** Este archivo solo señala puntos para dar los últimos retoques del programa.

## Antes de leer

- El análisis del código probó el backend (el servidor) y las pantallas **por separado**. Todavía no se probó que funcionen juntos. El informe, en cambio, dice que los 26 RF tienen "backend y pantalla conectados" (3.8.2). Por eso, aquí "coincide" quiere decir que cada parte, por separado, hace lo que dice el informe.
- "Cambiar el informe" quiere decir corregir el texto del documento Word. "Cambiar el código" quiere decir corregir el programa.

## 1. Lista de retoques para el programa

### 1.1 RF-26: registro del impacto (kilos de alimento y CO₂)

- **Informe:** «Realizado». Dice que después de cada recojo se registra el impacto "de forma conceptual (sin cálculo real)" (3.8.2).
- **Código:** «Incompleto». Sí se crea un registro por cada entrega, pero queda **vacío**: kilos y CO₂ sin valor, y `estimacionDisponible` siempre en falso. Ninguna pantalla muestra kilos ni CO₂ (lo que sí se muestra es el número de packs entregados).
- **Qué cambiar: el informe.** El código hace lo único que se puede hacer sin la fórmula, y el propio informe dice que la fórmula queda pendiente (1.4). Basta con explicar en 3.8.2 que hoy se guarda un registro vacío por cada entrega, listo para llenarse cuando exista la fórmula. El código se completará después, junto con la fórmula.

### 1.2 Aviso «Sin conexión» cuando falla internet

- **Código:** los inicios de sesión y el perfil muestran un aviso usando SweetAlert (una librería que dibuja ventanas de mensaje). Falla en dos lugares:
  - En el login del administrador, porque esa página no carga la librería (sale el error `Swal is not defined` y la persona no ve ningún mensaje).
  - En el perfil, cuando se llega desde el menú.
  
  Además, la librería se descarga de internet, así que si la página se abre sin internet, el aviso tampoco puede salir.
- **Informe:** no lo menciona.
- **Qué cambiar: el código.** Cargar la librería en esas dos pantallas (mejor aún: guardarla dentro del proyecto en vez de descargarla). El informe no necesita cambio.

### 1.3 La búsqueda filtra dentro del programa, no en la base de datos

- **Código:** el servidor trae las ofertas y las filtra él mismo. Con pocas ofertas es rápido; con muchas sería lento.
- **Informe:** da la medición de 15 ms (con un usuario y base H2) y deja pendiente probar con varios usuarios y con MySQL.
- **Qué cambiar: el código** (que el filtro lo haga la base de datos), antes de hacer las pruebas de rendimiento pendientes.

### 1.4 Delivery

- **Informe:** la integración con empresas de delivery queda pendiente; hoy solo hay recojo en el local.
- **Código:** la página pública muestra PedidosYa, Rappi e inDrive como "PROXIMAMENTE", pero también dice "elige la modalidad al comprar", y esa opción no existe ni en el carrito ni en el backend.
- **Qué cambiar: el código** (el texto de la página pública), para que no prometa algo que todavía no se puede hacer.

---