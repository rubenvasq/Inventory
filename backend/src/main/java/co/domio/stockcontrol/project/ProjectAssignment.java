package co.domio.stockcontrol.project;

import co.domio.stockcontrol.common.domain.BaseEntity;
import co.domio.stockcontrol.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Asignacion planificada de un producto a un proyecto: cantidad prevista y
 * acumulados de lo entregado y devuelto hasta el momento. Los acumulados solo
 * avanzan mediante movimientos de inventario vinculados al proyecto.
 */
@Entity
@Table(name = "project_assignment")
@Getter
@Setter
@NoArgsConstructor
public class ProjectAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Unidades previstas para el proyecto. */
    @Column(name = "assigned_quantity", nullable = false)
    private int assignedQuantity;

    /** Unidades efectivamente entregadas al proyecto (con descargo de stock). */
    @Column(name = "delivered_quantity", nullable = false)
    private int deliveredQuantity;

    /** Unidades devueltas del proyecto al inventario. */
    @Column(name = "returned_quantity", nullable = false)
    private int returnedQuantity;

    /** Unidades pendientes de entregar segun lo planificado. */
    public int pendingQuantity() {
        return assignedQuantity - deliveredQuantity;
    }
}
