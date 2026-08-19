-- Cada casilla produce hasta 3 actas independientes: una por elección
-- (Gubernatura, Diputación Local, Ayuntamiento), tal como en la práctica real
-- del IMPEPAC — son documentos físicos separados, no un acta combinada.
-- DFR R4/R5/R6: reportes y catálogos deben poder distinguirse por elección.

ALTER TABLE actas ADD COLUMN tipo_eleccion VARCHAR(30);
UPDATE actas SET tipo_eleccion = 'GUBERNATURA' WHERE tipo_eleccion IS NULL;
ALTER TABLE actas ALTER COLUMN tipo_eleccion SET NOT NULL;
CREATE INDEX idx_actas_tipo_eleccion ON actas(tipo_eleccion);

-- Antes era "una casilla = un acta"; ahora es "una casilla = un acta por elección".
ALTER TABLE actas DROP CONSTRAINT actas_casilla_id_key;
ALTER TABLE actas ADD CONSTRAINT actas_casilla_tipo_eleccion_key UNIQUE (casilla_id, tipo_eleccion);

ALTER TABLE cortes_publicacion ADD COLUMN tipo_eleccion VARCHAR(30);
UPDATE cortes_publicacion SET tipo_eleccion = 'GUBERNATURA' WHERE tipo_eleccion IS NULL;
ALTER TABLE cortes_publicacion ALTER COLUMN tipo_eleccion SET NOT NULL;
CREATE INDEX idx_cortes_tipo_eleccion ON cortes_publicacion(tipo_eleccion);
