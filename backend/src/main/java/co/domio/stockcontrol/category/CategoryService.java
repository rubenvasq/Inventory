package co.domio.stockcontrol.category;

import co.domio.stockcontrol.category.dto.CategoryRequest;
import co.domio.stockcontrol.category.dto.CategoryResponse;
import co.domio.stockcontrol.common.exception.DuplicateResourceException;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica de negocio para categorias.
 */
@Service
@Transactional
public class CategoryService {

    private static final String RESOURCE = "Categoria";

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAll().stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return repository.findById(id)
                .map(CategoryResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
    }

    public CategoryResponse create(CategoryRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Ya existe una categoria con el nombre '%s'".formatted(request.name()));
        }
        Category category = new Category();
        apply(category, request);
        return CategoryResponse.from(repository.save(category));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
        repository.findByNameIgnoreCase(request.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Ya existe una categoria con el nombre '%s'".formatted(request.name()));
                });
        apply(category, request);
        return CategoryResponse.from(repository.save(category));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.of(RESOURCE, id);
        }
        repository.deleteById(id);
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description());
        if (request.active() != null) {
            category.setActive(request.active());
        }
    }
}
