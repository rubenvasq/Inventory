package co.domio.stockcontrol.product;

import co.domio.stockcontrol.common.dto.PageResponse;
import co.domio.stockcontrol.product.dto.ProductRequest;
import co.domio.stockcontrol.product.dto.ProductResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Productos", description = "Gestion de productos y existencias")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Buscar productos con filtros y paginacion del lado del servidor")
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean lowStock,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.search(search, categoryId, lowStock, pageable);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Listar productos con existencias en o por debajo del minimo")
    public List<ProductResponse> lowStock() {
        return service.findLowStock();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un producto por id")
    public ProductResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear un producto (un stock inicial mayor que cero genera movimiento de apertura)")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un producto (no modifica las existencias)")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archivar un producto conservando su historial de movimientos")
    public ProductResponse archive(@PathVariable Long id) {
        return service.archive(id);
    }

    @PatchMapping("/{id}/restore")
    @Operation(summary = "Reactivar un producto archivado")
    public ProductResponse restore(@PathVariable Long id) {
        return service.restore(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un producto (rechazado si tiene movimientos; archiva en ese caso)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
