package co.domio.stockcontrol.dashboard;

import co.domio.stockcontrol.dashboard.dto.CategoryStockReport;
import co.domio.stockcontrol.dashboard.dto.DashboardSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores y reportes del inventario")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @Operation(summary = "Indicadores globales del inventario")
    public DashboardSummary summary() {
        return service.summary();
    }

    @GetMapping("/by-category")
    @Operation(summary = "Existencias y valor agrupados por categoria")
    public List<CategoryStockReport> byCategory() {
        return service.byCategory();
    }
}
