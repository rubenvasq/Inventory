package co.domio.stockcontrol.project.dto;

import co.domio.stockcontrol.project.Project;
import co.domio.stockcontrol.project.ProjectStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Representacion de salida de un proyecto, con sus asignaciones de material.
 */
public record ProjectResponse(
        Long id,
        String name,
        String client,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String notes,
        List<AssignmentResponse> assignments,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ProjectResponse from(Project project, List<AssignmentResponse> assignments) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getClient(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                project.getNotes(),
                assignments,
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
