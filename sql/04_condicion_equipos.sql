-- 04: condición física/operativa del equipo (independiente de su estado de movimiento).
-- Ejecutar una vez en MySQL Workbench (base almacen_sistema) ANTES de reiniciar la API.
USE almacen_sistema;

ALTER TABLE equipos
  ADD COLUMN condicion ENUM('OPERATIVO','SOLO_VENTA','NO_OPERATIVO','POR_REVISAR')
      NOT NULL DEFAULT 'POR_REVISAR' AFTER estado;

-- Clasificación inicial CONSERVADORA a partir de las observaciones ya cargadas.
-- Lo que no se puede asegurar queda "POR_REVISAR" para que el almacén lo revise con el equipo en la mano.
-- Cada regla pisa a la anterior (las fallas graves ganan al final).

-- 1) Dicen explícitamente que están bien
UPDATE equipos SET condicion = 'OPERATIVO'
 WHERE UPPER(observaciones) NOT LIKE '%NO OPERATIVO%'
   AND (UPPER(observaciones) LIKE '%OKEY%'
        OR UPPER(observaciones) LIKE '%OPERATIVO%'
        OR UPPER(observaciones) LIKE '%OPERATIVA%'
        OR UPPER(observaciones) LIKE '%BUEN ESTADO%');

-- 2) Funcionan pero no son para alquiler: marcados para venta, sin cargador, pantalla opaca/nubosa
UPDATE equipos SET condicion = 'SOLO_VENTA'
 WHERE UPPER(observaciones) LIKE '%(VENTA)%'
    OR UPPER(observaciones) LIKE '%NO TIENE CARGADOR%'
    OR UPPER(observaciones) LIKE '%OPAC%'
    OR UPPER(observaciones) LIKE '%NUBOS%';

-- 3) Fallas graves: no prenden / sin energía / pantalla rota
UPDATE equipos SET condicion = 'NO_OPERATIVO'
 WHERE UPPER(observaciones) LIKE '%NO PRENDE%'
    OR UPPER(observaciones) LIKE '%ENTRADA DE ENERGIA%'
    OR UPPER(observaciones) LIKE '%PUERTO DE ENERGIA%'
    OR UPPER(observaciones) LIKE '%PUERTO DE CARGA%'
    OR UPPER(observaciones) LIKE '%PANTALLA ROTA%'
    OR UPPER(observaciones) LIKE '%RUPTURA%';

-- Resumen para revisar
SELECT condicion, COUNT(*) AS total FROM equipos GROUP BY condicion ORDER BY total DESC;
