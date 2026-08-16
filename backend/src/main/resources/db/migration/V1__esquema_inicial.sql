-- =============================================================================
-- REDICAP – Esquema inicial PostgreSQL
-- DFR IMPEPAC Morelos | Flyway V1
-- =============================================================================

-- ── Catálogos geográfico-electorales ─────────────────────────────────────────
CREATE TABLE distritos (
    id      BIGSERIAL PRIMARY KEY,
    clave   VARCHAR(10)  NOT NULL UNIQUE,
    nombre  VARCHAR(100) NOT NULL,
    cabecera_municipal VARCHAR(100)
);

CREATE TABLE municipios (
    id      BIGSERIAL PRIMARY KEY,
    clave   VARCHAR(10)  NOT NULL UNIQUE,
    nombre  VARCHAR(100) NOT NULL
);

CREATE TABLE secciones (
    id              BIGSERIAL PRIMARY KEY,
    numero_seccion  INTEGER NOT NULL UNIQUE,
    municipio_id    BIGINT NOT NULL REFERENCES municipios(id),
    distrito_id     BIGINT NOT NULL REFERENCES distritos(id)
);

CREATE TABLE casillas (
    id              BIGSERIAL PRIMARY KEY,
    seccion_id      BIGINT  NOT NULL REFERENCES secciones(id),
    tipo            VARCHAR(20) NOT NULL,   -- BASICA, CONTIGUA, ESPECIAL, EXTRAORDINARIA
    numero_casilla  INTEGER NOT NULL,
    lista_nominal   INTEGER NOT NULL,
    activa          BOOLEAN NOT NULL DEFAULT true,
    UNIQUE (seccion_id, tipo, numero_casilla)
);

-- ── Actores políticos ─────────────────────────────────────────────────────────
CREATE TABLE partidos_politicos (
    id      BIGSERIAL PRIMARY KEY,
    siglas  VARCHAR(20)  NOT NULL UNIQUE,
    nombre  VARCHAR(150) NOT NULL,
    color_hex VARCHAR(7),
    activo  BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE coaliciones (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    tipo_eleccion   VARCHAR(30)  NOT NULL,
    activa          BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE coalicion_partidos (
    coalicion_id BIGINT NOT NULL REFERENCES coaliciones(id),
    partido_id   BIGINT NOT NULL REFERENCES partidos_politicos(id),
    PRIMARY KEY (coalicion_id, partido_id)
);

CREATE TABLE candidaturas (
    id                BIGSERIAL PRIMARY KEY,
    tipo_eleccion     VARCHAR(30)  NOT NULL,
    nombre_candidato  VARCHAR(200) NOT NULL,
    partido_id        BIGINT REFERENCES partidos_politicos(id),
    coalicion_id      BIGINT REFERENCES coaliciones(id),
    distrito_id       BIGINT REFERENCES distritos(id),
    municipio_id      BIGINT REFERENCES municipios(id),
    activa            BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT chk_candidatura_postulante CHECK (
        (partido_id IS NOT NULL AND coalicion_id IS NULL) OR
        (partido_id IS NULL AND coalicion_id IS NOT NULL)
    )
);

-- ── Usuarios ──────────────────────────────────────────────────────────────────
CREATE TABLE usuarios (
    id                   BIGSERIAL PRIMARY KEY,
    username             VARCHAR(20)  NOT NULL UNIQUE,
    password_hash        VARCHAR(200) NOT NULL,
    nombre_completo      VARCHAR(200) NOT NULL,
    curp                 VARCHAR(18),
    rol                  VARCHAR(30)  NOT NULL,
    casilla_asignada_id  BIGINT REFERENCES casillas(id),
    intentos_fallidos    INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta      TIMESTAMP,
    activo               BOOLEAN NOT NULL DEFAULT true,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_usuarios_username ON usuarios(username);

-- ── Actas ─────────────────────────────────────────────────────────────────────
CREATE TABLE actas (
    id                    BIGSERIAL PRIMARY KEY,
    casilla_id            BIGINT NOT NULL REFERENCES casillas(id),
    ruta_imagen           VARCHAR(500),
    hash_sha256           VARCHAR(64),
    estado                VARCHAR(30) NOT NULL DEFAULT 'RECIBIDA',
    digitalizador_id      BIGINT REFERENCES usuarios(id),
    recibida_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    publicada_at          TIMESTAMP,
    error_aritmetico      BOOLEAN NOT NULL DEFAULT false,
    excede_lista_nominal  BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (casilla_id)   -- una casilla = un acta por proceso
);
CREATE INDEX idx_actas_casilla ON actas(casilla_id);
CREATE INDEX idx_actas_estado  ON actas(estado);

-- ── Capturas (doble ciego) ────────────────────────────────────────────────────
CREATE TABLE capturas_acta (
    id                      BIGSERIAL PRIMARY KEY,
    acta_id                 BIGINT  NOT NULL REFERENCES actas(id),
    numero_captura          INTEGER NOT NULL CHECK (numero_captura IN (1, 2, 3)),
    capturista_id           BIGINT  NOT NULL REFERENCES usuarios(id),
    datos_votos_json        TEXT    NOT NULL,
    total_votos_acta        INTEGER,
    total_votos_calculado   INTEGER,
    capturado_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (acta_id, numero_captura)
);
CREATE INDEX idx_capturas_acta    ON capturas_acta(acta_id);
CREATE INDEX idx_capturas_usuario ON capturas_acta(capturista_id);

-- Regla: un mismo usuario NO puede capturar 2 veces la misma acta
CREATE UNIQUE INDEX idx_capturas_unique_usuario_acta
    ON capturas_acta(acta_id, capturista_id);

-- ── Log de auditoría (APPEND-ONLY) ───────────────────────────────────────────
CREATE TABLE log_auditoria (
    id                BIGSERIAL PRIMARY KEY,
    usuario_id        BIGINT REFERENCES usuarios(id),
    ip_origen         VARCHAR(45),
    tipo_accion       VARCHAR(30)  NOT NULL,
    modulo            VARCHAR(50)  NOT NULL,
    entidad_afectada  VARCHAR(100),
    valor_antes       TEXT,
    valor_despues     TEXT,
    timestamp_accion  TIMESTAMP(3) NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_log_usuario    ON log_auditoria(usuario_id);
CREATE INDEX idx_log_timestamp  ON log_auditoria(timestamp_accion);
CREATE INDEX idx_log_modulo     ON log_auditoria(modulo);

-- Revocar UPDATE y DELETE para garantizar append-only (DFR R9)
-- (ejecutar como superusuario al configurar la BD)
-- REVOKE UPDATE, DELETE ON log_auditoria FROM redicap_user;

-- ── Cortes de publicación ─────────────────────────────────────────────────────
CREATE TABLE cortes_publicacion (
    id                          BIGSERIAL PRIMARY KEY,
    generado_at                 TIMESTAMP NOT NULL DEFAULT NOW(),
    total_actas_capturadas      INTEGER NOT NULL,
    total_actas_validadas       INTEGER NOT NULL,
    total_casillas              INTEGER NOT NULL,
    porcentaje_participacion    DECIMAL(5,2),
    resultados_json             TEXT,
    exitoso                     BOOLEAN NOT NULL DEFAULT true
);
