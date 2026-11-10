package co.domio.stockcontrol.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.domio.stockcontrol.IntegrationTest;
import co.domio.stockcontrol.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el perfil propio del usuario autenticado: consulta, edicion del
 * nombre completo y cambio de contrasena. Ambos roles (ADMIN y OPERADOR)
 * pueden usarlo; el perfil nunca expone ni permite cambiar rol, username o
 * estado (eso sigue siendo /api/users, solo ADMIN).
 *
 * <p>Transaccional para no dejar usuarios en la base compartida: MockMvc
 * despacha en el mismo hilo, por lo que cada peticion se une a la transaccion
 * del test y se revierte al terminar.</p>
 */
@AutoConfigureMockMvc
@Transactional
class ProfileIT extends IntegrationTest {

    private static final String CURRENT_PASSWORD = "clave-de-prueba-123";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    /** Crea un usuario nuevo del rol indicado (se revierte al acabar) y devuelve su username. */
    private String newUser(Role role) {
        User user = new User();
        user.setUsername("perfil-" + role.name().toLowerCase() + "-" + System.nanoTime());
        user.setPassword(passwordEncoder.encode(CURRENT_PASSWORD));
        user.setFullName("Nombre Original");
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
        return user.getUsername();
    }

    @Test
    void ambosRolesConsultanYActualizanSuPerfil() throws Exception {
        for (Role role : Role.values()) {
            String username = newUser(role);
            String bearer = jwtService.generateToken(username, role.name());

            // Consulta: devuelve el propio perfil.
            mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + bearer))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.fullName").value("Nombre Original"))
                    .andExpect(jsonPath("$.role").value(role.name()));

            // Edicion: solo cambia el nombre completo; rol y estado intactos.
            mockMvc.perform(put("/api/profile").header("Authorization", "Bearer " + bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Nombre Editado\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.fullName").value("Nombre Editado"))
                    .andExpect(jsonPath("$.role").value(role.name()))
                    .andExpect(jsonPath("$.enabled").value(true));
        }
    }

    @Test
    void operadorNoAdministraUsuariosPeroSiSuPerfil() throws Exception {
        String bearer = jwtService.generateToken(newUser(Role.OPERADOR), "OPERADOR");

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + bearer))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + bearer))
                .andExpect(status().isOk());
    }

    @Test
    void cambioDeContrasenaVerificaLaActualYLaReemplaza() throws Exception {
        String username = newUser(Role.OPERADOR);

        // Contrasena actual incorrecta: 422, la contrasena no cambia.
        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", "Bearer " + jwtService.generateToken(username, "OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"equivocada\",\"newPassword\":\"nueva123456\"}"))
                .andExpect(status().isUnprocessableEntity());

        var stored = userRepository.findByUsername(username).orElseThrow();
        assertThat(passwordEncoder.matches(CURRENT_PASSWORD, stored.getPassword())).isTrue();

        // Contrasena actual correcta: se reemplaza y la anterior deja de validar.
        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", "Bearer " + jwtService.generateToken(username, "OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + CURRENT_PASSWORD + "\","
                                + "\"newPassword\":\"nueva123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        stored = userRepository.findByUsername(username).orElseThrow();
        assertThat(passwordEncoder.matches("nueva123456", stored.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(CURRENT_PASSWORD, stored.getPassword())).isFalse();
    }

    @Test
    void validacionesYAutenticacionDelPerfil() throws Exception {
        String bearer = jwtService.generateToken(newUser(Role.OPERADOR), "OPERADOR");

        // Sin token: 401.
        mockMvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());

        // Nombre vacio y contrasena nueva demasiado corta: 400 (validacion).
        mockMvc.perform(put("/api/profile").header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/profile/password").header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + CURRENT_PASSWORD + "\",\"newPassword\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }
}
