package co.domio.stockcontrol.movement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    List<StockMovement> findByProductIdOrderByIdDesc(Long productId);

    List<StockMovement> findByMovementTypeOrderByIdDesc(MovementType movementType);

    List<StockMovement> findAllByOrderByIdDesc();

    boolean existsByProductId(Long productId);

    boolean existsByProjectId(Long projectId);

    List<StockMovement> findByProjectIdOrderByIdDesc(Long projectId);
}
