package co.domio.stockcontrol.project;

/**
 * Estados del ciclo de vida de un proyecto o instalacion.
 */
public enum ProjectStatus {

    /** Planificado: aun no arranca la ejecucion. */
    PLANNED,

    /** En ejecucion: se puede entregar y devolver material. */
    IN_PROGRESS,

    /** Completado: cerrado; ya no admite entregas (las devoluciones siguen posibles). */
    COMPLETED,

    /** Cancelado: no admite entregas; conserva su historial y admite devoluciones. */
    CANCELLED
}
