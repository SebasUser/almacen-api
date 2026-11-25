# almacen-api

API del sistema de inventario del Almacén X (Tektronic). Spring Boot 4 · Java 21 · MySQL · JWT.

## Ejecutar en tu computadora
1. Crea la base de datos `almacen_sistema` y ejecuta, en orden, los scripts de la carpeta `sql/`
   (`01_CreacionBD.sql`, `02_importar_monitores.sql`, `03` … `07`; ver `CLAUDE.md`).
2. Copia `src/main/resources/local.properties.example` como `src/main/resources/local.properties`
   y completa tus valores. Ese archivo **no se sube a GitHub**.
3. Arranca con `mvnw.cmd spring-boot:run` (Windows) o desde IntelliJ. Queda en `http://localhost:8080`.

## Configuración (variables de entorno en la nube)
| Variable | Para qué |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a MySQL |
| `JWT_SECRET` | Clave de los tokens (mínimo 32 caracteres, aleatoria) |
| `ADMIN_PASSWORD` | Contraseña del administrador inicial (solo se usa si no hay usuarios) |
| `CORS_ORIGINS` | Dirección(es) del frontend, separadas por coma |
| `PORT` | Puerto (la nube lo define sola) |

Nunca escribas claves en el código ni en `application.properties`.
