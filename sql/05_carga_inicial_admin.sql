-- 05: atribuye al administrador la carga inicial de monitores (quedó sin usuario porque la importó un script).
-- Ejecutar una sola vez en MySQL Workbench. No cambia nada más.
USE almacen_sistema;

UPDATE movimientos
   SET usuario_id = (SELECT id FROM (SELECT MIN(id) AS id FROM usuarios WHERE rol = 'ADMIN') x)
 WHERE usuario_id IS NULL
   AND tipo = 'ENTRADA'
   AND origen_destino IN ('Carga inicial', 'Inventario inicial');

SELECT COUNT(*) AS cargas_atribuidas FROM movimientos WHERE origen_destino IN ('Carga inicial', 'Inventario inicial') AND usuario_id IS NOT NULL;
