-- ============================================================================
-- Migración 03: constancia de equipos eliminados y anulación de movimientos
-- Ejecutar UNA sola vez sobre la base almacen_sistema, ANTES de arrancar la API
-- (la API usa ddl-auto=validate y no arranca si faltan estas tablas/columnas).
-- Si aparece "Duplicate column name", la migración ya estaba aplicada.
-- ============================================================================
SET NAMES utf8mb4;
USE almacen_sistema;

-- 1) Constancia al eliminar un equipo (copia de sus datos y de su historial)
CREATE TABLE IF NOT EXISTS equipos_eliminados (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  equipo_id          BIGINT        NOT NULL,
  codigo_tk          VARCHAR(50)   NOT NULL,
  categoria          VARCHAR(50)   NOT NULL,
  numero_serie       VARCHAR(100),
  marca              VARCHAR(80),
  descripcion        VARCHAR(255),
  estado             VARCHAR(20)   NOT NULL,
  ubicacion          VARCHAR(200),
  total_movimientos  INT           NOT NULL DEFAULT 0,
  detalle            VARCHAR(8000) NOT NULL,
  motivo             VARCHAR(255)  NOT NULL,
  eliminado_por      VARCHAR(100)  NOT NULL,
  fecha_eliminacion  DATETIME      NOT NULL,
  INDEX idx_equipos_eliminados_fecha (fecha_eliminacion),
  INDEX idx_equipos_eliminados_codigo (codigo_tk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) Anulación de movimientos (el movimiento se conserva, marcado como anulado)
ALTER TABLE movimientos
  ADD COLUMN anulado          BOOLEAN      NOT NULL DEFAULT FALSE,
  ADD COLUMN anulado_por      VARCHAR(100) NULL,
  ADD COLUMN fecha_anulacion  DATETIME     NULL,
  ADD COLUMN motivo_anulacion VARCHAR(255) NULL;

-- Verificación: debe listar la tabla nueva y las 4 columnas nuevas
SHOW TABLES LIKE 'equipos_eliminados';
SHOW COLUMNS FROM movimientos LIKE '%anul%';
