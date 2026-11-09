package co.domio.stockcontrol.supplier;

import co.domio.stockcontrol.common.exception.DuplicateResourceException;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import co.domio.stockcontrol.supplier.dto.SupplierRequest;
import co.domio.stockcontrol.supplier.dto.SupplierResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica de negocio para proveedores.
 */
@Service
@Transactional
public class SupplierService {

    private static final String RESOURCE = "Proveedor";

    private final SupplierRepository repository;

    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        return repository.findAll().stream().map(SupplierResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(Long id) {
        return repository.findById(id)
                .map(SupplierResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
    }

    public SupplierResponse create(SupplierRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Ya existe un proveedor con el nombre '%s'".formatted(request.name()));
        }
        Supplier supplier = new Supplier();
        apply(supplier, request);
        return SupplierResponse.from(repository.save(supplier));
    }

    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
        repository.findByNameIgnoreCase(request.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Ya existe un proveedor con el nombre '%s'".formatted(request.name()));
                });
        apply(supplier, request);
        return SupplierResponse.from(repository.save(supplier));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.of(RESOURCE, id);
        }
        repository.deleteById(id);
    }

    private void apply(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.name().trim());
        supplier.setContactName(request.contactName());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        if (request.active() != null) {
            supplier.setActive(request.active());
        }
    }
}
