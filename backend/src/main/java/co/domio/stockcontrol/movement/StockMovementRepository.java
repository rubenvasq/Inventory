package co.domio.stockcontrol.movement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByIdDesc(Long productId);

    List<StockMovement> findByMovementTypeOrderByIdDesc(MovementType movementType);

    List<StockMovement> findAllByOrderByIdDesc();
}
