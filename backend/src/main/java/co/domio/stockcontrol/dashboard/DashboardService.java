package co.domio.stockcontrol.dashboard;

import co.domio.stockcontrol.category.CategoryRepository;
import co.domio.stockcontrol.dashboard.dto.CategoryStockReport;
import co.domio.stockcontrol.dashboard.dto.DashboardSummary;
import co.domio.stockcontrol.product.Product;
import co.domio.stockcontrol.product.ProductRepository;
import co.domio.stockcontrol.supplier.SupplierRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agrega metricas del inventario para el panel y los reportes.
 *
 * <p>El volumen de datos de un inventario de pyme es pequeno, por lo que las
 * agregaciones se calculan en memoria a partir de los productos; esto mantiene
 * la logica simple y verificable sin consultas agregadas complejas.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public DashboardService(ProductRepository productRepository, CategoryRepository categoryRepository,
            SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    public DashboardSummary summary() {
        List<Product> products = productRepository.findAll();
        long lowStock = products.stream()
                .filter(p -> p.getMinStock() > 0 && p.getStock() <= p.getMinStock())
                .count();
        long totalUnits = products.stream().mapToLong(Product::getStock).sum();
        BigDecimal totalValue = products.stream()
                .map(DashboardService::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardSummary(
                products.size(),
                categoryRepository.count(),
                supplierRepository.count(),
                lowStock,
                totalUnits,
                totalValue);
    }

    public List<CategoryStockReport> byCategory() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> p.getCategory().getId()))
                .values().stream()
                .map(group -> {
                    var category = group.get(0).getCategory();
                    long units = group.stream().mapToLong(Product::getStock).sum();
                    BigDecimal value = group.stream()
                            .map(DashboardService::valueOf)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new CategoryStockReport(category.getId(), category.getName(),
                            group.size(), units, value);
                })
                .sorted(Comparator.comparing(CategoryStockReport::categoryName))
                .toList();
    }

    private static BigDecimal valueOf(Product product) {
        if (product.getUnitPrice() == null) {
            return BigDecimal.ZERO;
        }
        return product.getUnitPrice().multiply(BigDecimal.valueOf(product.getStock()));
    }
}
