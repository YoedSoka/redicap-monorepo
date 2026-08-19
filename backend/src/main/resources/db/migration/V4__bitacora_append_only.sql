-- DFR R9 "Inmutabilidad Absoluta del Log": la bitácora de auditoría debe ser
-- imborrable incluso para el rol de Administrador. Un simple REVOKE no basta
-- porque redicap_user es dueño de la tabla (la creó al correr las migraciones)
-- y el dueño conserva sus privilegios implícitos sin importar los REVOKE.
-- Un trigger sí aplica a cualquier sesión, dueño incluido, sin excepción.
CREATE OR REPLACE FUNCTION fn_log_append_only() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'La bitácora de auditoría es de solo inserción (DFR R9)';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_append_only
    BEFORE UPDATE OR DELETE ON log_auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_log_append_only();
