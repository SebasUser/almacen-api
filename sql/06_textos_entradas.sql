-- 06: textos más claros en las entradas ya registradas.
-- Ejecutar una sola vez en MySQL Workbench.
USE almacen_sistema;

-- Los 109 monitores cargados desde el Excel
UPDATE movimientos
   SET origen_destino = 'Inventario inicial',
       observaciones  = 'Importado desde INVENTARIO_MONITORES.xlsx'
 WHERE tipo = 'ENTRADA' AND origen_destino = 'Carga inicial';

-- Antes, el alta manual guardaba el texto "Alta del equipo" como guía u observación
UPDATE movimientos
   SET guia = NULL
 WHERE tipo = 'ENTRADA' AND guia = 'Alta del equipo';

UPDATE movimientos
   SET observaciones = NULL
 WHERE tipo = 'ENTRADA' AND observaciones = 'Alta del equipo';

SELECT origen_destino, COUNT(*) AS total FROM movimientos WHERE tipo = 'ENTRADA' GROUP BY origen_destino;
