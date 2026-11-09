package co.domio.stockcontrol.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Valida la configuracion obligatoria del perfil de produccion y detiene el
 * arranque con un mensaje claro si falta algun valor o si conserva un valor
 * de desarrollo (secreto JWT por defecto, contrasena de administrador
 * conocida u origenes CORS locales).
 *
 * <p>La validacion nunca incluye el valor de los secretos en los mensajes:
 * solo nombra la variable que debe corregirse.</p>
 */
@Component
@Profile("prod")
public class ProductionConfigGuard {

    /** Valor por defecto del secreto JWT en desarrollo (application.yml). */
    static final String DEV_JWT_SECRET = "dev-secret-cambia-esto-por-favor-min-32-chars-0123456789";

    /** Contrasena de administrador con la que arranca el perfil dev. */
    static final String DEV_ADMIN_PASSWORD = "admin123";

    static final int MIN_JWT_SECRET_LENGTH = 32;
    static final int MIN_ADMIN_PASSWORD_LENGTH = 8;

    private final String jwtSecret;
    private final String adminUsername;
    private final String adminPassword;
    private final String[] corsOrigins;

    public ProductionConfigGuard(
            @Value("${app.jwt.secret:}") String jwtSecret,
            @Value("${app.admin.username:}") String adminUsername,
            @Value("${app.admin.password:}") String adminPassword,
            @Value("${app.cors.allowed-origins:}") String[] corsOrigins) {
        this.jwtSecret = jwtSecret;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.corsOrigins = corsOrigins;
    }

    @PostConstruct
    void validate() {
        List<String> problems = problems(jwtSecret, adminUsername, adminPassword, corsOrigins);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Configuracion de produccion invalida. Corrige lo siguiente y vuelve a arrancar:\n- "
                            + String.join("\n- ", problems));
        }
    }

    /**
     * Reglas de validacion (pureza para pruebas): devuelve la lista de
     * problemas encontrados; vacia significa configuracion valida.
     */
    static List<String> problems(String jwtSecret, String adminUsername, String adminPassword,
            String[] corsOrigins) {
        List<String> problems = new ArrayList<>();

        if (isBlank(jwtSecret)) {
            problems.add("JWT_SECRET no esta definido. Genera un secreto aleatorio de al menos "
                    + MIN_JWT_SECRET_LENGTH + " caracteres.");
        } else if (DEV_JWT_SECRET.equals(jwtSecret)) {
            problems.add("JWT_SECRET conserva el valor de desarrollo. Define un secreto propio para produccion.");
        } else if (jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            problems.add("JWT_SECRET es demasiado corto: usa al menos " + MIN_JWT_SECRET_LENGTH + " caracteres.");
        }

        if (isBlank(adminUsername)) {
            problems.add("ADMIN_USERNAME no esta definido.");
        }
        if (isBlank(adminPassword)) {
            problems.add("ADMIN_PASSWORD no esta definido. Define la contrasena del administrador inicial.");
        } else if (DEV_ADMIN_PASSWORD.equals(adminPassword)) {
            problems.add("ADMIN_PASSWORD conserva el valor de desarrollo ('" + DEV_ADMIN_PASSWORD
                    + "'). Define una contrasena distinta para produccion.");
        } else if (adminPassword.length() < MIN_ADMIN_PASSWORD_LENGTH) {
            problems.add("ADMIN_PASSWORD es demasiado corta: usa al menos " + MIN_ADMIN_PASSWORD_LENGTH
                    + " caracteres.");
        }

        if (corsOrigins == null || corsOrigins.length == 0 || allBlank(corsOrigins)) {
            problems.add("CORS_ALLOWED_ORIGINS no esta definido. Enumera explicitamente los origenes "
                    + "desde los que se sirve la interfaz (separados por coma).");
        } else {
            for (String origin : corsOrigins) {
                String value = origin == null ? "" : origin.trim();
                if (value.isEmpty()) {
                    continue;
                }
                if (isLocalOrigin(value)) {
                    problems.add("CORS_ALLOWED_ORIGINS contiene el origen local '" + value
                            + "', valido solo en desarrollo. Define origenes de produccion.");
                }
            }
        }
        return problems;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean allBlank(String[] values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLocalOrigin(String origin) {
        String lower = origin.toLowerCase();
        return lower.startsWith("http://localhost") || lower.startsWith("http://127.0.0.1")
                || lower.startsWith("https://localhost") || lower.startsWith("https://127.0.0.1");
    }
}
