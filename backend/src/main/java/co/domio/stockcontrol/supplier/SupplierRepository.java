package co.domio.stockcontrol.supplier;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Supplier> findByNameIgnoreCase(String name);
}
