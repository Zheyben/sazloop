### 1.2 La búsqueda filtra dentro del programa, no en la base de datos

- **Código:** el servidor trae las ofertas y las filtra él mismo. Con pocas ofertas es rápido; con muchas sería lento.
- **Informe:** da la medición de 15 ms (con un usuario y base H2) y deja pendiente probar con varios usuarios y con MySQL.
- **Qué cambiar: el código** (que el filtro lo haga la base de datos), antes de hacer las pruebas de rendimiento pendientes.

---