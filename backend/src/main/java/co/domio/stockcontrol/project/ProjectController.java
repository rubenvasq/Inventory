package co.domio.stockcontrol.project;

import co.domio.stockcontrol.common.dto.PageResponse;
import co.domio.stockcontrol.movement.dto.MovementResponse;
import co.domio.stockcontrol.project.dto.AssignmentRequest;
import co.domio.stockcontrol.project.dto.AssignmentResponse;
import co.domio.stockcontrol.project.dto.ProjectMaterialRequest;
import co.domio.stockcontrol.project.dto.ProjectRequest;
import co.domio.stockcontrol.project.dto.ProjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Proyectos", description = "Proyectos e instalaciones de clientes con asignacion y entrega de material")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Buscar proyectos con filtros y paginacion del lado del servidor")
    public PageResponse<ProjectResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProjectStatus status,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.search(search, status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un proyecto con sus asignaciones de material")
    public ProjectResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear un proyecto o instalacion")
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        ProjectResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/projects/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un proyecto, incluido su estado")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un proyecto (rechazado si tiene asignaciones o movimientos)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assignments")
    @Operation(summary = "Asignar (o ampliar la asignacion de) un producto al proyecto")
    public ResponseEntity<AssignmentResponse> assign(@PathVariable Long id,
            @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse created = service.assign(id, request);
        return ResponseEntity.created(URI.create("/api/projects/" + id + "/assignments/" + created.id()))
                .body(created);
    }

    @DeleteMapping("/{id}/assignments/{assignmentId}")
    @Operation(summary = "Retirar una asignacion sin entregas registradas")
    public ResponseEntity<Void> removeAssignment(@PathVariable Long id, @PathVariable Long assignmentId) {
        service.removeAssignment(id, assignmentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deliveries")
    @Operation(summary = "Entregar material al proyecto: descuenta stock y registra el movimiento")
    public ResponseEntity<MovementResponse> deliver(@PathVariable Long id,
            @Valid @RequestBody ProjectMaterialRequest request) {
        MovementResponse created = service.deliver(id, request);
        return ResponseEntity.created(URI.create("/api/projects/" + id + "/movements/" + created.id()))
                .body(created);
    }

    @PostMapping("/{id}/returns")
    @Operation(summary = "Devolver material del proyecto al inventario: reintegra stock y registra el movimiento")
    public ResponseEntity<MovementResponse> returnMaterial(@PathVariable Long id,
            @Valid @RequestBody ProjectMaterialRequest request) {
        MovementResponse created = service.returnMaterial(id, request);
        return ResponseEntity.created(URI.create("/api/projects/" + id + "/movements/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}/movements")
    @Operation(summary = "Historial de movimientos de inventario vinculados al proyecto")
    public List<MovementResponse> movements(@PathVariable Long id) {
        return service.findMovements(id);
    }
}
