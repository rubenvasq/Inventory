package co.domio.stockcontrol.user;

import co.domio.stockcontrol.common.exception.BusinessRuleException;
import co.domio.stockcontrol.common.exception.DuplicateResourceException;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import co.domio.stockcontrol.user.dto.ChangePasswordRequest;
import co.domio.stockcontrol.user.dto.CreateUserRequest;
import co.domio.stockcontrol.user.dto.UpdateProfileRequest;
import co.domio.stockcontrol.user.dto.UpdateUserRequest;
import co.domio.stockcontrol.user.dto.UserResponse;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion de usuarios. La administracion del catalogo completo es privativa
 * del rol ADMIN; cada usuario, sea ADMIN u OPERADOR, gestiona su propio
 * perfil y contrasena a traves de los metodos de perfil.
 */
@Service
@Transactional
public class UserService {

    private static final String RESOURCE = "Usuario";
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Perfil del usuario autenticado, por nombre de usuario (del token). */
    @Transactional(readOnly = true)
    public UserResponse findProfile(String username) {
        return repository.findByUsernameIgnoreCase(username)
                .map(UserResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, username));
    }

    /**
     * Actualiza el nombre completo del propio usuario. El rol, el estado y el
     * nombre de usuario quedan intactos (se gestionan como ADMIN).
     */
    public UserResponse updateOwnProfile(String username, UpdateProfileRequest request) {
        User user = requireUser(username);
        user.setFullName(request.fullName().trim());
        return UserResponse.from(repository.save(user));
    }

    /**
     * Cambia la contrasena del propio usuario verificando la actual. La nueva
     * se guarda cifrada con BCrypt y la anterior deja de validar.
     */
    public UserResponse changeOwnPassword(String username, ChangePasswordRequest request) {
        User user = requireUser(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessRuleException("La contrasena actual no es correcta");
        }
        if (request.newPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessRuleException(
                    "La nueva contrasena debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        return UserResponse.from(repository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return repository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
    }

    public UserResponse create(CreateUserRequest request) {
        if (repository.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateResourceException("Ya existe un usuario con el nombre '%s'".formatted(request.username()));
        }
        User user = new User();
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setEnabled(request.enabled() == null || request.enabled());
        return UserResponse.from(repository.save(user));
    }

    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));

        // Evita dejar el sistema sin ningun ADMIN activo.
        if (user.getRole() == Role.ADMIN
                && (request.role() != Role.ADMIN || Boolean.FALSE.equals(request.enabled()))
                && isLastActiveAdmin(user)) {
            throw new BusinessRuleException("No se puede degradar o desactivar al unico administrador activo");
        }

        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6) {
                throw new BusinessRuleException("La contrasena debe tener al menos 6 caracteres");
            }
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(repository.save(user));
    }

    public void delete(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
        if (user.getRole() == Role.ADMIN && isLastActiveAdmin(user)) {
            throw new BusinessRuleException("No se puede eliminar al unico administrador activo");
        }
        repository.deleteById(id);
    }

    private boolean isLastActiveAdmin(User candidate) {
        return repository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.isEnabled())
                .noneMatch(u -> !u.getId().equals(candidate.getId()));
    }

    /** Carga el usuario por nombre de usuario (viene del token, no de la URL). */
    private User requireUser(String username) {
        return repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, username));
    }
}
