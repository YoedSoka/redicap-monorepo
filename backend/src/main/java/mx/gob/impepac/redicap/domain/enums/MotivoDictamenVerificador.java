package mx.gob.impepac.redicap.domain.enums;

/**
 * Catálogo de motivos para toda resolución del Verificador, ya sea que apruebe
 * una de las 3 capturas o declare el acta ilegible (DFR R3: "Justificación Obligatoria").
 */
public enum MotivoDictamenVerificador {
    COINCIDENCIA_CLARA_CON_ACTA_FISICA,
    ERROR_DE_CAPTURA_EVIDENTE,
    IMAGEN_BORROSA_O_MOVIDA,
    OBSTRUCCION_O_DANO_FISICO,
    ILUMINACION_INSUFICIENTE,
    OTRO
}
