package co.domio.stockcontrol.project.dto;

import co.domio.stockcontrol.project.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Datos para crear o actualizar un proyecto o instalacion.
 */
public record ProjectRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String name,
        @NotBlank(message = "El cliente es obligatorio") @Size(max = 150) String client,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 500) String notes) {
}
