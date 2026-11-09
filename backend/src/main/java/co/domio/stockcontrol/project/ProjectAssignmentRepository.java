package co.domio.stockcontrol.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {

    Optional<ProjectAssignment> findByProjectIdAndProductId(Long projectId, Long productId);

    List<ProjectAssignment> findByProjectIdOrderByIdAsc(Long projectId);

    List<ProjectAssignment> findByProjectIdInOrderByIdAsc(List<Long> projectIds);

    boolean existsByProjectId(Long projectId);

    boolean existsByProductId(Long productId);
}
