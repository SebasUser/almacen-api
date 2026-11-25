-- 07: foto de perfil de cada usuario (imagen pequeña guardada como texto base64).
-- Ejecutar una sola vez en MySQL Workbench ANTES de reiniciar la API.
USE almacen_sistema;

ALTER TABLE usuarios ADD COLUMN foto VARCHAR(15000) NULL;
