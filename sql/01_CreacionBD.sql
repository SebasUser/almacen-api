CREATE DATABASE almacen_sistema
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE almacen_sistema;

-- Usuarios del sistema (login y roles)
CREATE TABLE usuarios (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre        VARCHAR(100) NOT NULL,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password      VARCHAR(255) NOT NULL,
  rol           ENUM('ADMIN','USUARIO') NOT NULL DEFAULT 'USUARIO',
  activo        BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Categorías: Monitor, Teclado, Mouse, Disco, RAM, PC...
CREATE TABLE categorias (
  id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre  VARCHAR(50) NOT NULL UNIQUE,
  activo  BOOLEAN NOT NULL DEFAULT TRUE
);

-- Campos propios de cada categoría (ej: Monitor -> Pulgadas)
CREATE TABLE categoria_atributos (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  categoria_id  BIGINT NOT NULL,
  nombre        VARCHAR(50) NOT NULL,
  UNIQUE (categoria_id, nombre),
  FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

-- Equipos: una sola tabla para todas las categorías
CREATE TABLE equipos (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  categoria_id        BIGINT NOT NULL,
  codigo_tk           VARCHAR(50) NOT NULL UNIQUE,
  numero_serie        VARCHAR(100),
  marca               VARCHAR(80),
  descripcion         VARCHAR(255),
  estado              ENUM('DISPONIBLE','ALQUILADO','VENDIDO','ASIGNADO','BAJA')
                      NOT NULL DEFAULT 'DISPONIBLE',
  ubicacion_tipo      ENUM('ALMACEN_X','CLIENTE','PERSONAL')
                      NOT NULL DEFAULT 'ALMACEN_X',
  ubicacion_detalle   VARCHAR(150),
  fecha_compra        DATE,
  proveedor           VARCHAR(150),
  factura             VARCHAR(50),
  guia                VARCHAR(50),
  costo_usd           DECIMAL(12,2),
  costo_pen           DECIMAL(12,2),
  tipo_cambio         DECIMAL(8,4),
  garantia            VARCHAR(100),
  observaciones       VARCHAR(255),
  created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

-- Valor de cada atributo por equipo (ej: equipo 5 -> Pulgadas = 24)
CREATE TABLE equipo_atributos (
  equipo_id    BIGINT NOT NULL,
  atributo_id  BIGINT NOT NULL,
  valor        VARCHAR(100) NOT NULL,
  PRIMARY KEY (equipo_id, atributo_id),
  FOREIGN KEY (equipo_id)   REFERENCES equipos(id),
  FOREIGN KEY (atributo_id) REFERENCES categoria_atributos(id)
);

-- Historial de movimientos de cada equipo
CREATE TABLE movimientos (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  equipo_id       BIGINT NOT NULL,
  tipo            ENUM('ENTRADA','SALIDA_ALQUILER','SALIDA_VENTA',
                       'ASIGNACION_PERSONAL','DEVOLUCION_CLIENTE','RETORNO_PERSONAL')
                  NOT NULL,
  fecha           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  origen_destino  VARCHAR(150),
  oc              VARCHAR(50),
  rq              VARCHAR(50),
  guia            VARCHAR(50),
  factura         VARCHAR(50),
  observaciones   VARCHAR(255),
  usuario_id      BIGINT,
  FOREIGN KEY (equipo_id)  REFERENCES equipos(id),
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Datos iniciales: categoría Monitor con su atributo Pulgadas
INSERT INTO categorias (nombre) VALUES ('Monitor');
INSERT INTO categoria_atributos (categoria_id, nombre)
  VALUES ((SELECT id FROM categorias WHERE nombre = 'Monitor'), 'Pulgadas');
  
  
  
SHOW TABLES;
SELECT * FROM categorias;