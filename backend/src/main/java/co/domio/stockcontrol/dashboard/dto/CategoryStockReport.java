package co.domio.stockcontrol.dashboard.dto;

import java.math.BigDecimal;

/**
 * Resumen de existencias y valor agrupado por categoria.
 *
 * @param categoryId   id de la categoria
 * @param categoryName nombre de la categoria
 * @param productCount numero de productos en la categoria
 * @param totalUnits   suma de existencias de esos productos
 * @param totalValue   valor total (sum de stock * precio) de la categoria
 */
public record CategoryStockReport(
        Long categoryId,
        String categoryName,
        long productCount,
        long totalUnits,
        BigDecimal totalValue) {
}
