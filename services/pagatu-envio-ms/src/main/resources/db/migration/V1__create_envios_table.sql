CREATE TABLE IF NOT EXISTS envios (
    id BIGSERIAL PRIMARY KEY,
    orden_id BIGINT NOT NULL,
    direccion_envio VARCHAR(255) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    guia_seguimiento VARCHAR(100),
    motivo_rechazo VARCHAR(255),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_envios_orden UNIQUE (orden_id)
);

