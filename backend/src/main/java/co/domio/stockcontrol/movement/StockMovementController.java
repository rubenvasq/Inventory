package co.domio.stockcontrol.movement;

import co.domio.stockcontrol.movement.dto.MovementRequest;
import co.domio.stockcontrol.movement.dto.MovementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Movimientos", description = "Registro de entradas, salidas y ajustes de inventario")
public class StockMovementController {

    private final StockMovementService service;

    public StockMovementController(StockMovementService service) {
        this.service = service;
    }

    @PostMapping("/api/products/{productId}/movements")
    @Operation(summary = "Registrar un movimiento y actualizar las existencias del producto")
    public ResponseEntity<MovementResponse> register(@PathVariable Long productId,
            @Valid @RequestBody MovementRequest request) {
        MovementResponse created = service.register(productId, request);
        return ResponseEntity
                .created(URI.create("/api/products/" + productId + "/movements/" + created.id()))
                .body(created);
    }

    @GetMapping("/api/products/{productId}/movements")
    @Operation(summary = "Listar los movimientos de un producto (mas recientes primero)")
    public List<MovementResponse> byProduct(@PathVariable Long productId) {
        return service.findByProduct(productId);
    }

    @GetMapping("/api/movements")
    @Operation(summary = "Listar todos los movimientos, opcionalmente filtrados por tipo")
    public List<MovementResponse> all(@RequestParam(required = false) MovementType type) {
        return service.findAll(type);
    }
}
