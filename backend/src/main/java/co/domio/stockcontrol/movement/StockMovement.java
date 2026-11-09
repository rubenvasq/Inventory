package co.domio.stockcontrol.movement;

import co.domio.stockcontrol.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Movimiento de inventario. Registro inmutable: una vez creado no se modifica ni
 * se elimina (no extiende BaseEntity porque no necesita marca de actualizacion).
 */
@Entity
@Table(name = "stock_movement")
@Getter
@Setter
@NoArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "previous_stock", nullable = false)
    private int previousStock;

    @Column(name = "new_stock", nullable = false)
    private int newStock;

    @Column(name = "note", length = 255)
    private String note;

    /**
     * Proyecto al que pertenece el movimiento, cuando se origino en la entrega
     * o devolucion de material a un proyecto. Se guarda como identificador
     * plano para no acoplar este libro mayor con la entidad de proyecto.
     */
    @Column(name = "project_id", updatable = false)
    private Long projectId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
