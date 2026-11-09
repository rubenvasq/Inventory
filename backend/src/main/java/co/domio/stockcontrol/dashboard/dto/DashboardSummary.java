package co.domio.stockcontrol.dashboard.dto;

import java.math.BigDecimal;

/**
 * Indicadores globales del inventario para el panel principal.
 *
 * @param totalProducts        numero de productos registrados
 * @param totalCategories      numero de categorias
 * @param totalSuppliers       numero de proveedores
 * @param lowStockCount        productos en o por debajo del stock minimo
 * @param totalStockUnits      suma de existencias de todos los productos
 * @param totalInventoryValue  valor total del inventario (sum de stock * precio)
 */
public record DashboardSummary(
        long totalProducts,
        long totalCategories,
        long totalSuppliers,
        long lowStockCount,
        long totalStockUnits,
        BigDecimal totalInventoryValue) {
}
