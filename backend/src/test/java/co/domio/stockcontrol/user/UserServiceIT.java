package co.domio.stockcontrol.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.domio.stockcontrol.IntegrationTest;
import co.domio.stockcontrol.common.exception.BusinessRuleException;
import co.domio.stockcontrol.common.exception.DuplicateResourceException;
import co.domio.stockcontrol.user.dto.CreateUserRequest;
import co.domio.stockcontrol.user.dto.UpdateUserRequest;
import co.domio.stockcontrol.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UserServiceIT extends IntegrationTest {

    @Autowired
    UserService service;
    @Autowired
    UserRepository repository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void crea_conContrasenaCifrada() {
        UserResponse u = service.create(new CreateUserRequest(
                "oper-" + System.nanoTime(), "secreta123", "Operador", Role.OPERADOR, true));

        var stored = repository.findById(u.id()).orElseThrow();
        assertThat(stored.getPassword()).isNotEqualTo("secreta123");
        assertThat(passwordEncoder.matches("secreta123", stored.getPassword())).isTrue();
    }

    @Test
    void usernameDuplicado_lanzaDuplicate() {
        String username = "dup-" + System.nanoTime();
        service.create(new CreateUserRequest(username, "secreta123", "Uno", Role.OPERADOR, true));

        assertThatThrownBy(() -> service.create(
                new CreateUserRequest(username, "secreta123", "Dos", Role.OPERADOR, true)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void noPermiteDegradarAlUnicoAdmin() {
        // El unico ADMIN es el sembrado por DataInitializer. Degradarlo debe fallar.
        var admin = repository.findAll().stream()
                .filter(x -> x.getRole() == Role.ADMIN && x.isEnabled())
                .findFirst().orElseThrow();

        assertThatThrownBy(() -> service.update(admin.getId(),
                new UpdateUserRequest("Admin", Role.OPERADOR, true, null)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
