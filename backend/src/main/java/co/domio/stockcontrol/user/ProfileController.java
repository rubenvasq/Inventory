package co.domio.stockcontrol.user;

import co.domio.stockcontrol.user.dto.ChangePasswordRequest;
import co.domio.stockcontrol.user.dto.UpdateProfileRequest;
import co.domio.stockcontrol.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfil del usuario autenticado. Cada usuario (ADMIN u OPERADOR) consulta y
 * edita su propio perfil; no usa /api/users/** porque esa ruta es solo ADMIN.
 */
@RestController
@RequestMapping("/api/profile")
@Tag(name = "Perfil", description = "Perfil propio del usuario autenticado")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Datos del usuario autenticado")
    public UserResponse profile(Principal principal) {
        return userService.findProfile(principal.getName());
    }

    @PutMapping
    @Operation(summary = "Actualizar el nombre completo del perfil propio")
    public UserResponse updateProfile(Principal principal, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateOwnProfile(principal.getName(), request);
    }

    @PutMapping("/password")
    @Operation(summary = "Cambiar la contrasena propia (exige la actual)")
    public UserResponse changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        return userService.changeOwnPassword(principal.getName(), request);
    }
}
