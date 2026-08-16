package mx.gob.impepac.redicap.domain.enums;

/** Roles del sistema REDICAP (DFR R8). */
public enum RolUsuario {
    ADMINISTRADOR,
    DIGITALIZADOR,   // ~800 en campo (app móvil)
    CAPTURISTA,      // doble ciego (web)
    VERIFICADOR,     // mesa de deliberación (web)
    CONSULTOR_PUBLICO
}
