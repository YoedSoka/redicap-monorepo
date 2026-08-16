-- La BD real quedó "baselined" en V1 sin que V1__esquema_inicial.sql se ejecutara de verdad,
-- así que en algún momento se agregó a mano la columna `datos_votos` (la que usa CapturaActa.java)
-- pero nunca se eliminó la columna original `datos_votos_json` (NOT NULL) de V1.
-- Resultado: todo INSERT real desde la app fallaba por violar el NOT NULL de datos_votos_json.
-- Este script deja el esquema correcto sin importar si se parte de la BD real (ya con datos_votos)
-- o de una BD nueva construida desde cero (V1 -> V2).

ALTER TABLE capturas_acta ADD COLUMN IF NOT EXISTS datos_votos TEXT;

UPDATE capturas_acta
   SET datos_votos = datos_votos_json
 WHERE datos_votos IS NULL AND datos_votos_json IS NOT NULL;

ALTER TABLE capturas_acta ALTER COLUMN datos_votos SET NOT NULL;
ALTER TABLE capturas_acta DROP COLUMN IF EXISTS datos_votos_json;

-- Ya existía sin trackear en la BD real; se documenta aquí para que un ambiente nuevo quede igual.
CREATE UNIQUE INDEX IF NOT EXISTS idx_capturas_unique_usuario_acta
    ON capturas_acta (acta_id, capturista_id);
