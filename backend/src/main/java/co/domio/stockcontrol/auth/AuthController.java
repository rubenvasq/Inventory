package co.domio.stockcontrol.auth;

import co.domio.stockcontrol.auth.dto.LoginRequest;
import co.domio.stockcontrol.auth.dto.LoginResponse;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import co.domio.stockcontrol.security.JwtService;
import co.domio.stockcontrol.user.User;
import co.domio.stockcontrol.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacion", description = "Inicio de sesion y usuario actual")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion y obtener un token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // Lanza AuthenticationException si las credenciales son invalidas (-> 401).
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", request.username()));
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMinutes(),
                user.getUsername(), user.getFullName(), user.getRole().name());
    }

    @GetMapping("/me")
    @Operation(summary = "Datos del usuario autenticado")
    public LoginResponse me(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", principal.getName()));
        return new LoginResponse(null, "Bearer", jwtService.getExpirationMinutes(),
                user.getUsername(), user.getFullName(), user.getRole().name());
    }
}
