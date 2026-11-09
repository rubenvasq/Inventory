package co.domio.stockcontrol.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.domio.stockcontrol.IntegrationTest;
import co.domio.stockcontrol.user.Role;
import co.domio.stockcontrol.user.User;
import co.domio.stockcontrol.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica que las reglas de autorizacion se aplican en el backend: un
 * OPERADOR no puede ejecutar operaciones reservadas a ADMIN llamando
 * directamente a la API, aunque la interfaz no se lo muestre.
 *
 * <p>Transaccional para no dejar usuarios en la base compartida: MockMvc
 * despacha en el mismo hilo, por lo que cada peticion se une a la transaccion
 * del test y se revierte al terminar.</p>
 */
@AutoConfigureMockMvc
@Transactional
class AuthorizationIT extends IntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private String token(Role role) {
        User user = new User();
        user.setUsername("auth-" + role.name().toLowerCase() + "-" + System.nanoTime());
        user.setPassword(passwordEncoder.encode("clave-de-prueba-123"));
        user.setFullName("Usuario de prueba");
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
        return jwtService.generateToken(user.getUsername(), role.name());
    }

    private void perform(RequestBuilder request, int expectedStatus) throws Exception {
        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    @Test
    void operadorNoPuedeCrearNiEditarNiEliminarCatalogos() throws Exception {
        String token = token(Role.OPERADOR);

        perform(post("/api/products").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"X\",\"name\":\"X\",\"categoryId\":1}"), 403);

        perform(put("/api/products/1").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"X\",\"name\":\"X\",\"categoryId\":1}"), 403);

        perform(delete("/api/products/1").header("Authorization", "Bearer " + token), 403);

        perform(post("/api/categories").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Nueva\"}"), 403);

        perform(delete("/api/suppliers/1").header("Authorization", "Bearer " + token), 403);

        perform(post("/api/projects").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"P\",\"client\":\"C\"}"), 403);
    }

    @Test
    void operadorNoPuedeAdministrarUsuariosNiAsignaciones() throws Exception {
        String token = token(Role.OPERADOR);

        perform(post("/api/users").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nuevo\",\"password\":\"clave123\",\"fullName\":\"N\","
                        + "\"role\":\"OPERADOR\"}"), 403);

        perform(post("/api/projects/1/assignments").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":5}"), 403);
    }

    @Test
    void operadorPuedeConsultarYRegistrarMovimientos() throws Exception {
        String token = token(Role.OPERADOR);

        // Consultas permitidas para ambos roles.
        perform(get("/api/products").header("Authorization", "Bearer " + token), 200);
        perform(get("/api/dashboard/summary").header("Authorization", "Bearer " + token), 200);
        perform(get("/api/movements").header("Authorization", "Bearer " + token), 200);
        perform(get("/api/projects").header("Authorization", "Bearer " + token), 200);

        // Registrar movimientos de inventario es OPERADOR-friendly: la peticion
        // pasa la autorizacion y falla con 404 porque el producto no existe.
        perform(post("/api/products/999999/movements").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"IN\",\"quantity\":5}"), 404);

        // Entregar/devolver material a proyectos tambien es una operacion de
        // inventario: 404 (proyecto inexistente) demuestra que paso la regla.
        perform(post("/api/projects/999999/deliveries").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":5}"), 404);
    }

    @Test
    void adminPuedeCrearUsuariosYPeticionSinTokenSeRechaza() throws Exception {
        perform(post("/api/users").header("Authorization", "Bearer " + token(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin-creado-" + System.nanoTime()
                        + "\",\"password\":\"clave123\",\"fullName\":\"N\",\"role\":\"OPERADOR\"}"), 201);

        perform(get("/api/products"), 401);
    }
}
