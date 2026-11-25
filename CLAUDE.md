# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

REST API for warehouse equipment inventory ("Almacén X"): equipment (`Equipo`) grouped by category, with per-category dynamic attributes and a movement history (rentals, sales, staff assignments, returns). Spring Boot 4.1 / Java 21 / Maven / MySQL / JWT auth. Code, identifiers, and error messages are in Spanish — keep new code consistent with that. The intended client is an Angular frontend at `http://localhost:4200` (default CORS origin; configurable with `CORS_ORIGINS`).

## Commands

Use the Maven wrapper (`mvnw.cmd` on Windows, `./mvnw` in bash):

```
./mvnw spring-boot:run                 # run on :8080
./mvnw clean package                   # build jar (runs tests)
./mvnw clean package -DskipTests
./mvnw test
./mvnw test -Dtest=AlmacenApiApplicationTests            # single class
./mvnw test -Dtest=AlmacenApiApplicationTests#contextLoads   # single method
```

The only existing test is a `@SpringBootTest` context-load test, which needs a reachable MySQL with the schema in place (there is no H2/test profile).

## Database

- MySQL `almacen_sistema` on `localhost:3306`. **No secrets in `application.properties`**: it only has `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`, `${JWT_SECRET}`, `${ADMIN_PASSWORD}`, `${CORS_ORIGINS}` (default `http://localhost:4200`), `${SHOW_SQL}` and `${PORT}`. Locally they come from `src/main/resources/local.properties` (gitignored; copy `local.properties.example`); in the cloud they are environment variables with the same names. The app fails to start if a required one is missing.
- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate never creates or alters tables. The schema lives **outside this project** in `sql/01_CreacionBD.sql` (tables `usuarios`, `categorias`, `categoria_atributos`, `equipos`, `equipo_atributos`, `movimientos`, plus seed categories/attributes). `sql/02_importar_monitores.sql` is a data import for monitors. Incremental migrations live in `sql/` inside this project: `sql/03_auditoria_y_anulaciones.sql` (table `equipos_eliminados` and the `anulado*` columns of `movimientos`) must be run once, after `sql/01_CreacionBD.sql`, on every database (local and hosted). `sql/04_condicion_equipos.sql` (column `equipos.condicion` + initial classification of the imported monitors from their observaciones) must be run next, also on every database. Any entity change must be matched by a manual change to that SQL — otherwise startup fails validation.
- `open-in-view=false`: all lazy associations must be loaded inside service `@Transactional` methods (or via `@EntityGraph` on repositories, as `EquipoRepository.findAll(spec, pageable)` and `MovimientoRepository` do). Map entities to DTOs inside the transaction.
- `AdminInicial` creates user `admin` (password from `app.admin.password`) on startup only when the `usuarios` table is empty.

## Architecture

Layered: `controller` → `service` → `repository` (Spring Data JPA) with `entity` (Lombok) and `dto` (Java records). Controllers are thin; some simple read endpoints (`CategoriaController`) talk to repositories directly.

**Security** (`config/SecurityConfig`, `security/`): stateless JWT. `POST /api/auth/login` is the only public endpoint; it returns a token with the username as subject and a `rol` claim. `JwtAuthFilter` reloads the user from the DB on every request, so deactivated/deleted users are rejected even with a valid token. Roles come from the `Rol` enum and are checked with `@PreAuthorize("hasRole('ADMIN')")` (method security is enabled) — e.g. editing/deleting equipment is ADMIN-only, while creating equipment and registering movements is open to any authenticated user. The acting user is taken from `Authentication.getName()` and recorded on movements.

**Errors**: throw `ApiException.noEncontrado/badRequest/conflicto(...)` from services; `GlobalExceptionHandler` turns it, bean-validation failures, and `DataIntegrityViolationException` into a JSON error response.

**Pagination**: list endpoints accept Spring `Pageable` params (`page`, `size`, `sort`) and return the custom `PaginaResponse<T>` record rather than Spring's `Page`. The equipment list also filters by registration date (`desde` / `hasta`, ISO dates `yyyy-MM-dd`, both inclusive, applied to `createdAt`) and accepts `sort` for ordering (the frontend sends `createdAt,desc` + `id,desc` by default).

**Dynamic attributes**: each `Categoria` defines allowed `CategoriaAtributo` names (e.g. Monitor → "Pulgadas"). An `Equipo` stores values in `EquipoAtributo` (composite key equipo+atributo). The API exchanges them as a `Map<String,String>` keyed by attribute name (case-insensitive lookup); `EquipoService.sincronizarAtributos` adds/updates/removes rows to match the map, rejecting unknown names. An equipment's category cannot change after creation. Filtering by attribute (currently only `pulgadas`) is done with an `EXISTS` subquery in `EquipoSpecs`.

**Equipment state machine** (`MovimientoService.registrar`): equipment state (`EstadoEquipo`) and location (`UbicacionTipo` + `ubicacionDetalle`) are changed **only** by registering a `Movimiento`, never by editing the equipment directly. The equipment row is locked with `PESSIMISTIC_WRITE` (`EquipoRepository.buscarParaActualizar`) while the transition is applied:

| TipoMovimiento | Required state | Resulting state / location |
|---|---|---|
| `ENTRADA` | — (created automatically by `EquipoService.crear`; rejected via API) | `DISPONIBLE` / `ALMACEN_X` |
| `SALIDA_ALQUILER` | `DISPONIBLE` | `ALQUILADO` / `CLIENTE` (origenDestino required) |
| `SALIDA_VENTA` | `DISPONIBLE`, `ALQUILADO` | `VENDIDO` / `CLIENTE` (origenDestino required) |
| `ASIGNACION_PERSONAL` | `DISPONIBLE` | `ASIGNADO` / `PERSONAL` (origenDestino required) |
| `DEVOLUCION_CLIENTE` | `ALQUILADO` | `DISPONIBLE` / `ALMACEN_X` |
| `RETORNO_PERSONAL` | `ASIGNADO` | `DISPONIBLE` / `ALMACEN_X` |

`VENDIDO` and `BAJA` are terminal (no further movements). Movement dates may not be in the future (5‑minute tolerance). Deleting an equipment (ADMIN only, `DELETE /api/equipos/{id}?motivo=...`, reason of at least 5 characters is mandatory) first saves a snapshot of the equipment and its full history into `equipos_eliminados` (who, when, why) and then deletes its movements; the log is readable via `GET /api/auditoria/equipos-eliminados` (ADMIN only).

**Batch intake (from almacén M)**: `POST /api/ingresos/lote` (`IngresoService`) takes a category, origin (default "Almacén M"), guide, notes and up to 200 items (code, serial, brand, description, attributes). Per code, in one transaction: unknown code -> `EquipoService.crearConEntrada` (new equipment + ENTRADA movement with that origin/guide); existing ALQUILADO -> DEVOLUCION_CLIENTE; existing ASIGNADO -> RETORNO_PERSONAL; existing DISPONIBLE/VENDIDO/BAJA -> rejected. Any failure rolls back the whole batch and names the code.

**Scanning / batch**: `GET /api/equipos/codigo?valor=...` does an exact, case-insensitive lookup by `codigoTk` (then by `numeroSerie` if unique) for barcode scanners (labels encode the TK code). `POST /api/movimientos/lote` (body = a movement plus `equipoIds`, max 200) registers the same movement for several equipments in one transaction by reusing `MovimientoService.registrar`; if any equipment does not admit it, nothing is saved and the error names the equipment.

**User management** (ADMIN only, `UsuarioController`): `PUT /api/usuarios/{id}` can also correct a user's `username` (unique, stored lower-case; movements reference the user id so history is unaffected, but the person's token stops working and they must log in again). `POST /api/usuarios/{id}/transferir-administracion` (body `{accion: DEJAR_COMO_USUARIO | DESACTIVAR, passwordActual}`) makes the chosen active user ADMIN and, in the same transaction, demotes (or deactivates) the caller after confirming the caller's password; this is the only way an admin can stop being admin, so the system never ends up without an active admin.

**Movement corrections**: movements are never edited or deleted. An ADMIN can void the latest still-valid movement of an equipment with `POST /api/movimientos/{id}/anular` (body `{motivo}`, at least 5 characters). The movement stays in the history flagged `anulado` (with `anuladoPor`, `fechaAnulacion`, `motivoAnulacion`) and the equipment state/location is restored from the previous valid movement (`MovimientoService.restaurarEstado`). The initial `ENTRADA` cannot be voided; older movements must be voided newest-first.

**Normalization conventions** in services: strings are trimmed and blank → `null` (`limpiar`); `codigoTk` and `marca` are stored upper-case; `codigoTk` uniqueness is checked case-insensitively.

**Equipment condition** (`CondicionEquipo`: `OPERATIVO`, `SOLO_VENTA`, `NO_OPERATIVO`, `POR_REVISAR`; column `equipos.condicion`, default `POR_REVISAR`): physical/operational condition, independent of the movement state. It is set via `EquipoRequest.condicion` (create, ADMIN edit, and per intake item; `null` = keep current / `POR_REVISAR` on creation) and filterable with `GET /api/equipos?condicion=`. `MovimientoService.registrar` enforces it: `SALIDA_ALQUILER` and `ASIGNACION_PERSONAL` require `OPERATIVO`; `SALIDA_VENTA` is rejected only for `NO_OPERATIVO` (an admin must reclassify it to `SOLO_VENTA` first). Returns (devolución/retorno) are never blocked. `observaciones` are deliberately not filterable; the UI highlights them in the equipment sheet.

**Movements list filters**: `GET /api/movimientos` accepts `q` (equipment code/serial, origenDestino, OC, RQ, guía, factura — `MovimientoSpecs`), `tipo`, `usuarioId`, `anulado` (true/false), `desde`/`hasta` (ISO dates on `fecha`, inclusive). `GET /api/movimientos/usuarios` lists the users who registered movements (for the "Registrado por" filter).
