package co.domio.stockcontrol.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Prueba unitaria de las reglas de configuracion de produccion (sin contexto
 * de Spring): valores faltantes o de desarrollo deben detener el arranque.
 */
class ProductionConfigGuardTest {

    private static final String SECRET_OK = "s".repeat(48);
    private static final String PASSWORD_OK = "clave-de-produccion";
    private static final String[] ORIGINS_OK = {"https://app.domio.co"};

    @Test
    void configuracionValida_noReportaProblemas() {
        assertThat(ProductionConfigGuard.problems(SECRET_OK, "admin", PASSWORD_OK, ORIGINS_OK)).isEmpty();
    }

    @Test
    void valoresFaltantes_reportanLasTresVariablesObligatorias() {
        List<String> problems = ProductionConfigGuard.problems(null, null, null, null);

        assertThat(problems).hasSize(4);
        assertThat(problems.get(0)).contains("JWT_SECRET");
        assertThat(problems.get(1)).contains("ADMIN_USERNAME");
        assertThat(problems.get(2)).contains("ADMIN_PASSWORD");
        assertThat(problems.get(3)).contains("CORS_ALLOWED_ORIGINS");
    }

    @Test
    void valoresDeDesarrollo_rechazados() {
        List<String> problems = ProductionConfigGuard.problems(
                ProductionConfigGuard.DEV_JWT_SECRET, "admin",
                ProductionConfigGuard.DEV_ADMIN_PASSWORD,
                new String[] {"http://localhost:5173"});

        assertThat(problems).hasSize(3);
        assertThat(problems.get(0)).contains("JWT_SECRET");
        assertThat(problems.get(1)).contains("ADMIN_PASSWORD");
        assertThat(problems.get(2)).contains("localhost");
    }

    @Test
    void secretoDemasiadoCorto_rechazado() {
        List<String> problems = ProductionConfigGuard.problems(
                "corto", "admin", PASSWORD_OK, ORIGINS_OK);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("al menos 32 caracteres");
    }

    @Test
    void contrasenaCorta_rechazada() {
        List<String> problems = ProductionConfigGuard.problems(
                SECRET_OK, "admin", "corta1", ORIGINS_OK);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("al menos 8 caracteres");
    }

    @Test
    void origenesVaciosOLocales_rechazados() {
        assertThat(ProductionConfigGuard.problems(SECRET_OK, "admin", PASSWORD_OK, new String[] {}))
                .hasSize(1);
        assertThat(ProductionConfigGuard.problems(
                SECRET_OK, "admin", PASSWORD_OK, new String[] {"http://127.0.0.1:5173"}))
                .hasSize(1);
    }
}
