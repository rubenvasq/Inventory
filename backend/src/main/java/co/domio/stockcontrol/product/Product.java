package co.domio.stockcontrol.product;

import co.domio.stockcontrol.category.Category;
import co.domio.stockcontrol.common.domain.BaseEntity;
import co.domio.stockcontrol.supplier.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Producto del inventario: equipo o material gestionado por DOMIO.
 * Las existencias ({@code stock}) se fijan al crear y, a partir de la Etapa 4,
 * solo se modifican mediante movimientos de inventario.
 */
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Column(name = "sku", nullable = false, length = 60, unique = true)
    private String sku;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit = "unidad";

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "stock", nullable = false)
    private int stock = 0;

    @Column(name = "min_stock", nullable = false)
    private int minStock = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
