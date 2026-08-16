package mx.gob.impepac.redicap.domain.enums;

/**
 * Máquina de estados de un Acta de Escrutinio y Cómputo.
 *
 * Flujo principal (DFR R2 / R3):
 *   RECIBIDA → EN_CAPTURA_1 → EN_CAPTURA_2
 *     ├─ coinciden → VALIDADA → (corte) → PUBLICADA
 *     └─ difieren  → EN_CAPTURA_3
 *          ├─ C3 == C1 o C3 == C2 → VALIDADA
 *          └─ tres distintas       → MESA_DELIBERACION → VALIDADA_VERIFICADOR | ILEGIBLE
 */
public enum EstadoActa {
    RECIBIDA,
    EN_CAPTURA_1,
    EN_CAPTURA_2,
    EN_CAPTURA_3,
    MESA_DELIBERACION,
    VALIDADA,
    VALIDADA_VERIFICADOR,
    ILEGIBLE,
    PUBLICADA
}
