# Sazloop

Este es un proyecto académico universitario del curso "Desarrollo Web Integrado" basado en el tema N.º 91 "SalvemosLaComida". Los establecimientos ofrecen excedentes de comida aptos para consumo envueltos en packs sorpresa con descuento para los consumidores.

## Requisitos para una computadora nueva y opciones de prueba del proyecto

- Windows con **JDK 21**.
- MySQL versión 8.0.

### Opción A: H2 con datos

Creado para ver una demostración de las cuentas ya registradas para todos los portales y evitar crear cuentas uno por uno. Esto se realizó para probar la plataforma y las funcionalidades que trae.

Desde la ruta `C:\Users\Emanuel\Documents\Proyectos\Sazloop\backend` abrimos el **CMD** y ejecutamos el siguiente comando:

```bat
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev,demo
```

Cuando aparezca `Tomcat started on port 8080`, abre [http://localhost:8080/](http://localhost:8080/). El sitio público y los portales se abren desde esa misma dirección.
Utiliza los siguientes accesos para ingresar a las cuentas:

| Acceso | Establecimiento o persona | Usuario o correo | Contraseña |
| --- | --- | --- | --- |
| Administrador | Administración de prueba | `demo.admin@sazloop.test` | `SazloopDemo2026!` |
| Empresa | Aurora Cocina (Miraflores y San Borja) | `duena.aurora@sazloop.test` | `SazloopDemo2026!` |
| Empresa | Oriente Pan (Ate) | `dueno.oriente@sazloop.test` | `SazloopDemo2026!` |
| Empresa | Sazón Urbano (Ate) | `duena.sazon@sazloop.test` | `SazloopDemo2026!` |
| Trabajador | Aurora Miraflores | `demo.miraflores` | `SazloopDemo2026!` |
| Trabajador | Oriente Pan Ate | `demo.ayllon1` | `SazloopDemo2026!` |
| Trabajador | Sazón Urbano Ate | `demo.ayllon2` | `SazloopDemo2026!` |
| Consumidor | Valeria Quispe | `demo.consumidor1@sazloop.test` | `SazloopDemo2026!` |
| Consumidor | Diego Flores | `demo.consumidor2@sazloop.test` | `SazloopDemo2026!` |

La demo carga **tres empresas, cuatro establecimientos, dos consumidores y tres ofertas**, una por cada establecimiento activo. Aurora San Borja queda pendiente de revisión administrativa, sin cuenta de trabajador ni oferta hasta que se apruebe.

## Opción B: MySQL local

Inicia MySQL y ejecuta una sola vez en MySQL Workbench o en la consola de MySQL:

```sql
CREATE DATABASE IF NOT EXISTS sazloop;
```

### Configurar MySQL

Tu usuario y contraseña de MySQL **no se escriben en el repositorio**. Cada integrante los guarda en un archivo local que Git ignora:

1. En la carpeta `backend/`, copia el archivo `application-local.properties.example`.
2. A la copia quítale el `.example` para que se llame `application-local.properties`.
3. Ábrelo y escribe tu usuario y tu contraseña de MySQL:

```properties
spring.datasource.username=root
spring.datasource.password=TU_CONTRASEÑA_DE_MYSQL
```

4. Arranca el backend **desde la carpeta `backend/`**: `mvnw.cmd spring-boot:run` (el perfil `mysql` es el predeterminado). El archivo se busca en la carpeta desde la que arrancas.

`application-local.properties` está en el `.gitignore`: nunca se sube a GitHub. Si no existe, el perfil `mysql` usa los valores de ejemplo de `application-mysql.properties` (`root` / `cambia_esto`) y no podrá conectarse. El perfil `dev` (H2) no lee este archivo.

Para ingresar al administrador debes ingresar estas credenciales:
- Email: admin@correo.com
- Contraseña: admin123

Abre [http://localhost:8080/](http://localhost:8080/) cuando arranque Tomcat. Las tablas se crean o actualizan automáticamente. **MySQL conserva sus datos** al detener la aplicación, pero una instalación nueva no recibe las cuentas de H2 ni las de otra computadora. La cuenta administradora se crea solamente si el correo indicado todavía no existe en esa base; cambiar la variable después no modifica su contraseña.
